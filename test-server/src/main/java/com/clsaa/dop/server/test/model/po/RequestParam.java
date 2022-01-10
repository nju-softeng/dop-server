package com.clsaa.dop.server.test.model.po;

import com.clsaa.dop.server.test.enums.ParamClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author xihao
 * @version 1.0
 * @since 04/05/2019
 */
@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "interface_request_params", schema = "db_dop_test",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"request_id", "name"})},
        indexes = {@Index(columnList = "request_id,name", unique = true)})
public class RequestParam implements Po {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------- main property ---------
    @Enumerated(value = EnumType.STRING)
    private ParamClass paramClass;

    private String name;

    private String value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private RequestScript requestScript;

    // ----------- common property ---------
    private LocalDateTime ctime;

    private LocalDateTime mtime;

    private Long cuser;

    private Long muser;

    @Column(name = "is_deleted")
    private boolean deleted;
}
