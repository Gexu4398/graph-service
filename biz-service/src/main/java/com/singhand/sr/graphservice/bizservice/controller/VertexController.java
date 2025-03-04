package com.singhand.sr.graphservice.bizservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("vertex")
@Tag(name = "实体和实体的关系")
@Validated
public class VertexController {

}
