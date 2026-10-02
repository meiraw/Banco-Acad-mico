package br.wm.banco.academico.DTOs.Response;

public class LoginResponseDTO {

        private String token;

        public LoginResponseDTO(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
}
