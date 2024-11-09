package com.miapp.modulocontrataciones.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            logger.error("Error occurred: Status code = {}", statusCode);

            if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                model.addAttribute("status", statusCode);
                model.addAttribute("statusText", HttpStatus.BAD_REQUEST.getReasonPhrase());
                return "error/400";
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("status", statusCode);
                model.addAttribute("statusText", HttpStatus.NOT_FOUND.getReasonPhrase());
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("status", statusCode);
                model.addAttribute("statusText", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
                return "error/500";
            }
        }
        model.addAttribute("status", status);
        model.addAttribute("statusText", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return "error/error";
    }
}
