package com.clsaa.dop.server.permission.model.bo;

import lombok.*;


import java.time.LocalDateTime;
/**
 * 功能点业务层对象
 *
 * @author lzy
 *

 *
 * @since :2019.3.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionBoV1 {
/*
 *    功能点ID
 *    父功能点ID
 *    功能点名称
 *    是否私有
 *    功能点描述
 *    创建时间
 *    修改时间
 *    创建人
 *    修改人
 *    删除标记
 */
    private Long id;


    private Long parentId;

    private String name;

    private Integer isPrivate;

    private String description;

    /* 表里都要有的字段*/

    private LocalDateTime ctime;

    private LocalDateTime mtime;

    private Long cuser;

    private Long muser;

    private Boolean deleted;
    /* 表里都要有的字段*/
}
