package com.ponti.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ponti.api.model.TipoUsuario;
import com.ponti.api.model.Usuario;
import com.ponti.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// Criação da conta de admin a partir das variáveis de ambiente (sem senha no código).
// Este contexto sobe SEM as variáveis (mesmo que o terminal tenha PONTI_ADMIN_EMAIL/SENHA definidas:
// as propriedades do teste valem mais que as variáveis de ambiente).
@SpringBootTest(properties = {"ponti.admin.email=", "ponti.admin.senha="})
@Transactional
class ContaAdminTest {

    @Autowired ContaAdmin contaAdmin;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void semAsVariaveisNaoExisteAdmin() {
        assertThat(usuarioRepository.findAll()).noneMatch(u -> u.getTipo() == TipoUsuario.ADMIN);
        assertThat(contaAdmin.garantir("", "")).isNull();
        assertThat(contaAdmin.garantir("admin@ponti.com", null)).isNull();
    }

    @Test
    void criaOAdminComEmailNormalizadoESenhaEmHash() {
        Usuario admin = contaAdmin.garantir("  Equipe@Ponti.com ", "uma-senha-bem-longa");
        assertThat(admin).isNotNull();
        assertThat(admin.getEmail()).isEqualTo("equipe@ponti.com");
        assertThat(admin.getTipo()).isEqualTo(TipoUsuario.ADMIN);
        assertThat(admin.getStartupId()).isNull();
        assertThat(admin.getMentorId()).isNull();
        assertThat(passwordEncoder.matches("uma-senha-bem-longa", admin.getSenhaHash())).isTrue();
    }

    @Test
    void senhaCurtaNaoCriaAConta() {
        assertThat(contaAdmin.garantir("equipe@ponti.com", "curta")).isNull();
        assertThat(usuarioRepository.findByEmail("equipe@ponti.com")).isEmpty();
    }

    @Test
    void trocarASenhaNaVariavelAtualizaOHash() {
        contaAdmin.garantir("equipe@ponti.com", "senha-antiga-123");
        Usuario admin = contaAdmin.garantir("equipe@ponti.com", "senha-nova-456789");
        assertThat(passwordEncoder.matches("senha-nova-456789", admin.getSenhaHash())).isTrue();
        assertThat(passwordEncoder.matches("senha-antiga-123", admin.getSenhaHash())).isFalse();
        assertThat(usuarioRepository.findAll().stream().filter(u -> u.getTipo() == TipoUsuario.ADMIN)).hasSize(1);
    }

    @Test
    void nuncaTransformaUmaContaDeStartupEmAdmin() {
        assertThat(contaAdmin.garantir("petcare@exemplo.com", "uma-senha-bem-longa")).isNull();
        Usuario petcare = usuarioRepository.findByEmail("petcare@exemplo.com").orElseThrow();
        assertThat(petcare.getTipo()).isEqualTo(TipoUsuario.STARTUP);
        assertThat(passwordEncoder.matches(DadosIniciais.SENHA_EXEMPLO, petcare.getSenhaHash())).isTrue();
    }
}
