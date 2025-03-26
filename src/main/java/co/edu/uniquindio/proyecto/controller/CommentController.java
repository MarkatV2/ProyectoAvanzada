package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.service.implementations.CommentServiceImplements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/comments")
@Slf4j
public class CommentController {

    private final CommentServiceImplements commentService;
    //CREAR LA AUTENTICCION MEJOR YA
}
