package com.hibiscus.docs.core.annotations;

/**
 * 更新用户请求模型
 */
@ApiModel(name = "UpdateUserRequest", description = "更新用户请求模型")
public class UpdateUserRequest {

    @ApiParam(name = "name", description = "用户姓名", type = "string", example = "李四")
    private String name;

    @ApiParam(name = "email", description = "用户邮箱", type = "string", format = "email", example = "lisi@example.com")
    private String email;

    @ApiParam(name = "age", description = "用户年龄", type = "number", min = "0", max = "150", example = "30")
    private Integer age;

    @ApiParam(name = "active", description = "是否激活", type = "boolean", example = "true")
    private Boolean active;

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
