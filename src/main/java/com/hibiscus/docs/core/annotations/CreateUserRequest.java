package com.hibiscus.docs.core.annotations;

import java.util.List;

/**
 * 创建用户请求模型
 */
@ApiModel(name = "CreateUserRequest", description = "创建用户请求模型",
        example = "{\"name\":\"张三\",\"email\":\"zhangsan@example.com\",\"age\":25}")
public class CreateUserRequest {

    @ApiParam(name = "name", description = "用户姓名", type = "string", required = true, example = "张三")
    private String name;

    @ApiParam(name = "email", description = "用户邮箱", type = "string", format = "email", required = true, example = "zhangsan@example.com")
    private String email;

    @ApiParam(name = "age", description = "用户年龄", type = "number", min = "0", max = "150", example = "25")
    private Integer age;

    @ApiParam(name = "phone", description = "用户电话", type = "string", pattern = "^1[3-9]\\d{9}$", example = "13800138000")
    private String phone;

    @ApiParam(name = "tags", description = "用户标签", type = "array", example = "[\"VIP\",\"活跃用户\"]")
    private List<String> tags;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
