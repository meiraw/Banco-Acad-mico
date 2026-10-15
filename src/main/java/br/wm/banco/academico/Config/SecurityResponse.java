package br.wm.banco.academico.Config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

final class SecurityResponse {
    private SecurityResponse() { }
    static void write(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String mensagem = status == 401 ? "Autenticacao necessaria ou token invalido" : "Acesso negado";
        response.getWriter().write("{\"mensagem\":\"" + mensagem + "\",\"status\":" + status + "}");
    }
}
