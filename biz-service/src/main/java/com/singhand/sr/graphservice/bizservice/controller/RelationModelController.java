package com.singhand.sr.graphservice.bizservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("relationModel")
@Tag(name = "关系模型管理")
@Validated
public class RelationModelController {

}
