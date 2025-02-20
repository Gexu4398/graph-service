package com.singhand.sr.graphservice.bizservice.controller;

import com.singhand.sr.graphservice.bizkeycloakmodel.model.Role;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.NewRoleRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.model.request.RenameRoleRequest;
import com.singhand.sr.graphservice.bizkeycloakmodel.repository.KeycloakRoleRepository;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakRoleService;
import com.singhand.sr.graphservice.bizkeycloakmodel.service.KeycloakService;
import com.singhand.sr.graphservice.bizkeycloakmodel.validator.NotSuperAdminRole;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.bizlogger.BizLogger;
import com.singhand.sr.graphservice.bizservice.aspect.annotation.resolver.Resolve;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("role")
@Slf4j
@Validated
public class RoleController {

  private final KeycloakRoleService roleService;

  private final KeycloakService keycloakService;

  private final KeycloakRoleRepository roleRepository;

  @Autowired
  public RoleController(KeycloakRoleService roleService, KeycloakService keycloakService,
      KeycloakRoleRepository roleRepository) {

    this.roleService = roleService;
    this.keycloakService = keycloakService;
    this.roleRepository = roleRepository;
  }

  @RequestMapping(method = RequestMethod.HEAD)
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public ResponseEntity<?> statRole(@RequestParam String roleName) {

    if (roleRepository.existsByNameAndClientRole(roleName, true)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @BizLogger(
      module = @Resolve("'角色管理'"),
      type = "新建",
      contentFormat = "新建角色【%s】",
      contentFormatArguments = @Resolve("response.name"),
      targetId = @Resolve("response.name"),
      targetName = @Resolve("response.name"),
      targetType = @Resolve("'角色'"))
  @PostMapping
  @Operation(summary = "新建角色")
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public Role newRole(@Valid @RequestBody NewRoleRequest newRoleRequest) {

    return roleService.newRole(newRoleRequest);
  }

  @BizLogger(
      module = @Resolve("'角色管理'"),
      type = "编辑",
      contentFormat = "编辑角色【%s】",
      contentFormatArguments = @Resolve("response.name"),
      targetId = @Resolve("response.name"),
      targetName = @Resolve("response.name"),
      targetType = @Resolve("'角色'"))
  @PutMapping("{roleName}")
  @Operation(summary = "编辑角色")
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public Role updateRole(@NotSuperAdminRole @PathVariable String roleName,
      @RequestBody Set<String> scopes) {

    return roleService.updateRole(roleName, scopes);
  }

  @GetMapping
  @Operation(summary = "获取角色")
  @PreAuthorize("isAuthenticated()")
  public List<Role> getRoles() {

    return roleService.getRoles()
        .stream()
        .sorted(Comparator
            .comparing((Role role) -> role.getName().equals("超级管理员") ? 0 : 1)
            .thenComparing(Role::getName)
        ).toList();
  }

  @GetMapping("{roleName}")
  @Operation(summary = "获取角色详情")
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public Role getRole(@PathVariable String roleName) {

    return roleService.getRole(roleName);
  }

  @BizLogger(
      module = @Resolve("'角色管理'"),
      type = "删除",
      contentFormat = "删除角色【%s】",
      contentFormatArguments = @Resolve("request.path.roleName"),
      targetId = @Resolve("request.path.roleName"),
      targetName = @Resolve("request.path.roleName"),
      targetType = @Resolve("'角色'"))
  @DeleteMapping("{roleName}")
  @Operation(summary = "删除角色")
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public void deleteRole(@NotSuperAdminRole @PathVariable String roleName) {

    keycloakService.getClientRoleResource(roleName).remove();
  }

  @BizLogger(
      module = @Resolve("'角色管理'"),
      type = "编辑",
      contentFormat = "重命名角色【%s】",
      contentFormatArguments = @Resolve("request.body.newRoleName"),
      targetId = @Resolve("request.body.newRoleName"),
      targetName = @Resolve("request.body.newRoleName"),
      targetType = @Resolve("'角色'"))
  @PostMapping("{roleName}:rename")
  @Operation(summary = "重命名角色")
  @PreAuthorize("hasAnyAuthority('role:crud')")
  public Role renameRole(@NotSuperAdminRole @PathVariable String roleName,
      @RequestBody RenameRoleRequest renameRoleRequest) {

    return roleName.equals(renameRoleRequest.getNewRoleName()) ? roleService.getRole(roleName)
        : roleService.renameRole(roleName, renameRoleRequest.getNewRoleName());
  }
}
