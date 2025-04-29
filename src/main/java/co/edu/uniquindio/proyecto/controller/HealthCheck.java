package co.edu.uniquindio.proyecto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/v1/helth")
@RequiredArgsConstructor
@Slf4j
public class HealthCheck {

  @GetMapping()
  public ResponseEntity<Boolean> checkSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return ResponseEntity.ok(session != null);
  }
}
