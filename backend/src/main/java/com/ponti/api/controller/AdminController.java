package com.ponti.api.controller;

import com.ponti.api.dto.DashboardAdmin;
import com.ponti.api.dto.PaginaAdmin;
import com.ponti.api.dto.PedidoBloqueio;
import com.ponti.api.dto.PedidoDesbloqueio;
import com.ponti.api.dto.UsuarioAdmin;
import com.ponti.api.model.RegistroAuditoria;
import com.ponti.api.model.TipoUsuario;
import com.ponti.api.service.AdminUsuariosService;
import com.ponti.api.service.DashboardService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

// Área do administrador. A configuração de segurança libera /admin/** só para a conta ADMIN.
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminUsuariosService usuariosService;
    private final DashboardService dashboardService;

    public AdminController(AdminUsuariosService usuariosService, DashboardService dashboardService) {
        this.usuariosService = usuariosService;
        this.dashboardService = dashboardService;
    }

    // Dashboard: ?inicio=2026-11-01&fim=2026-11-27 (padrão: últimos 30 dias). Compara com o período
    // anterior de mesmo tamanho.
    @GetMapping("/dashboard")
    public DashboardAdmin dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboardService.gerar(inicio, fim);
    }

    // Lista de usuários: ?busca=&tipo=STARTUP|MENTOR|ADMIN&situacao=ATIVO|BLOQUEADO&pagina=0&tamanho=20
    @GetMapping("/usuarios")
    public PaginaAdmin<UsuarioAdmin> usuarios(@RequestParam(required = false) String busca,
                                              @RequestParam(required = false) TipoUsuario tipo,
                                              @RequestParam(required = false) AdminUsuariosService.Situacao situacao,
                                              @RequestParam(required = false) Integer pagina,
                                              @RequestParam(required = false) Integer tamanho) {
        return usuariosService.listar(busca, tipo, situacao, pagina, tamanho);
    }

    @PostMapping("/usuarios/{id}/bloquear")
    public UsuarioAdmin bloquear(@PathVariable Long id, @Valid @RequestBody PedidoBloqueio body) {
        return usuariosService.bloquear(id, body.motivo());
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    public UsuarioAdmin desbloquear(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) PedidoDesbloqueio body) {
        return usuariosService.desbloquear(id, body == null ? null : body.motivo());
    }

    // Registro de auditoria, mais recentes primeiro
    @GetMapping("/auditoria")
    public PaginaAdmin<RegistroAuditoria> auditoria(@RequestParam(required = false) Integer pagina,
                                                    @RequestParam(required = false) Integer tamanho) {
        return usuariosService.auditoria(pagina, tamanho);
    }
}
