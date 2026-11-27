package com.ponti.api.config;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cria (ou atualiza) a conta de administrador ao subir a API.
// O e-mail e a senha vêm das variáveis de ambiente PONTI_ADMIN_EMAIL e PONTI_ADMIN_SENHA
// (no Render, em Environment). Nada disso fica no código. Sem as duas variáveis, não existe admin.
// Trocar a senha é só mudar a variável e reiniciar: na subida, o hash é atualizado.
@Component
@Order(0)
public class ContaAdmin implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ContaAdmin.class);
    static final int TAMANHO_MINIMO_SENHA = 10;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final String email;
    private final String senha;

    public ContaAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, DataSource dataSource,
                      @Value("${ponti.admin.email:}") String email,
                      @Value("${ponti.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        liberarTipoAdminNoPostgres();
        garantir(email, senha);
    }

    // Devolve o admin criado/atualizado, ou null se a configuração estiver faltando ou for inválida.
    Usuario garantir(String emailConfigurado, String senhaConfigurada) {
        if (emailConfigurado == null || emailConfigurado.isBlank() || senhaConfigurada == null || senhaConfigurada.isBlank()) {
            log.info("Conta de administrador não configurada (defina PONTI_ADMIN_EMAIL e PONTI_ADMIN_SENHA).");
            return null;
        }
        if (senhaConfigurada.length() < TAMANHO_MINIMO_SENHA) {
            log.error("PONTI_ADMIN_SENHA precisa ter pelo menos {} caracteres. Conta de administrador não criada.",
                    TAMANHO_MINIMO_SENHA);
            return null;
        }
        String emailNormalizado = Usuario.normalizarEmail(emailConfigurado);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado).orElse(null);

        if (usuario != null && usuario.getTipo() != TipoUsuario.ADMIN) {
            // Nunca transforma uma conta de startup ou mentor em admin
            log.error("O e-mail de PONTI_ADMIN_EMAIL já pertence a uma conta de {}. Conta de administrador não criada.",
                    usuario.getTipo());
            return null;
        }
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setEmail(emailNormalizado);
            usuario.setTipo(TipoUsuario.ADMIN);
            usuario.setSenhaHash(passwordEncoder.encode(senhaConfigurada));
            log.info("Conta de administrador criada.");
            return usuarioRepository.save(usuario);
        }
        if (!passwordEncoder.matches(senhaConfigurada, usuario.getSenhaHash())) {
            usuario.setSenhaHash(passwordEncoder.encode(senhaConfigurada));
            log.info("Senha da conta de administrador atualizada.");
            return usuarioRepository.save(usuario);
        }
        return usuario;
    }

    // O Hibernate cria a coluna "tipo" com uma regra (CHECK) que só aceita os valores que existiam
    // quando a tabela nasceu (STARTUP e MENTOR). Com ddl-auto=update essa regra não é atualizada,
    // então no PostgreSQL de produção o INSERT do admin seria recusado. Por isso a regra antiga é
    // removida aqui (o próprio enum em Java continua garantindo os valores válidos).
    private void liberarTipoAdminNoPostgres() {
        try (Connection conexao = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(conexao.getMetaData().getDatabaseProductName())) {
                return;
            }
        } catch (Exception e) {
            log.warn("Não foi possível conferir o banco: {}", e.getMessage());
            return;
        }
        new JdbcTemplate(dataSource).execute("ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_tipo_check");
    }
}
