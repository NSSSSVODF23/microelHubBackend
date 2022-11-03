package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.HttpResponse;
import com.microel.microelhub.api.transport.LoginRequest;
import com.microel.microelhub.security.AuthenticationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("api/public")
public class PublicResolvers {

    private final AuthenticationManager authenticationManager;

    public PublicResolvers(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("login")
    private ResponseEntity<HttpResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(HttpResponse.of(authenticationManager.doLogin(request)));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @PostMapping("refresh-token")
    private ResponseEntity<HttpResponse> refreshToken(@RequestBody String token){
        try {
            return ResponseEntity.ok(HttpResponse.of(authenticationManager.doRefresh(authenticationManager.validateRefreshToken(token))));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }
}
