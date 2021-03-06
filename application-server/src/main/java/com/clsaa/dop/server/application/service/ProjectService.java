package com.clsaa.dop.server.application.service;

import com.clsaa.dop.server.application.config.BizCodes;
import com.clsaa.dop.server.application.config.PermissionConfig;
import com.clsaa.dop.server.application.dao.ProjectRepository;
import com.clsaa.dop.server.application.model.bo.ProjectBoV1;
import com.clsaa.dop.server.application.model.po.Project;
import com.clsaa.dop.server.application.model.vo.ProjectV1;
import com.clsaa.dop.server.application.model.vo.UserV1;
import com.clsaa.dop.server.application.util.BeanUtils;
import com.clsaa.rest.result.Pagination;
import com.clsaa.rest.result.bizassert.BizAssert;
import com.clsaa.rest.result.bizassert.BizCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(value = "projectService")
public class ProjectService {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserService userService;
    @Autowired
    ImageService imageService;
    @Autowired
    private PermissionConfig permissionConfig;

    @Autowired
    private PermissionService permissionService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void deleteMemberFromProject(Long userId, Long projectId, Long loginUser) {
//        BizAssert.authorized(this.permissionService.check(permissionConfig.getDeleteMemberFromProject(), loginUser, permissionConfig.getProjectRuleFieldName(), projectId)
//                , BizCodes.NO_PERMISSION);
        logger.info("[deleteMemberFromProject] Request coming: loginUser={}, projectId={}, userId={}",loginUser,projectId,userId);
        this.permissionService.deleteByFieldAndUserId(projectId, this.permissionConfig.getProjectRuleFieldName(), userId);
    }

    public void addMemberToProject(List<Long> userIdList, Long projectId, Long loginUser) {
//        BizAssert.authorized(this.permissionService.check(permissionConfig.getAddMemberToProject(), loginUser, permissionConfig.getProjectRuleFieldName(), projectId)
//                , BizCodes.NO_PERMISSION);
//        System.out.println("userIdList = " + userIdList + ", projectId = " + projectId + ", loginUser = " + loginUser+", this.permissionConfig.getProjectRuleFieldName() = "+this.permissionConfig.getProjectRuleFieldName());
        logger.info("[addMemberToProject] Request coming: loginUser={}, projectId={}, userIdList={}",loginUser,projectId,userIdList);
        List<Long> existUserIdList = this.permissionService.getProjectMembers(this.permissionConfig.getProjectRuleFieldName(), projectId);
        Set<Long> userIdSet = new HashSet<>(existUserIdList);


        for (Long userId : userIdList) {

//            BizAssert.validParam(!existUserIdList.contains(userId), new BizCode(BizCodes.INVALID_PARAM.getCode(), "??????" + String.valueOf(userId) + "???????????????"));
            try {
//                System.out.println("??????userId:"+userId+"????????????");
                this.permissionService.addData(this.permissionConfig.getDeveloperAndProjectRuleId(), Long.valueOf(userId), projectId, loginUser);
            } catch (Exception e) {
                logger.error("[addMemberToProject] ???????????????????????????Exception",e);
//                System.out.println("????????????");
                BizAssert.justFailed(new BizCode(BizCodes.INVALID_PARAM.getCode(), "??????" + String.valueOf(userId) + "??????????????????????????????????????????"));
            }
            //this.permissionService.addRoleToUser(userId,this.permissionConfig.get);
        }



    }

    public List<UserV1> getMembersInProject(Long projectId) {
        logger.info("[getMembersInProject] Request coming: projectId={}",projectId);
        List<Long> userIdList = this.permissionService.getProjectMembers(this.permissionConfig.getProjectRuleFieldName(), projectId);
        return userIdList.stream().map(l -> this.userService.findUserById(l)).collect(Collectors.toList());
    }

    /**
     * ??????????????????
     *
     * @param pageNo          ??????
     * @param pageSize        ?????????
     * @param includeFinished ????????????????????????
     * @param queryKey        ???????????????
     * @return {@link Pagination<ProjectBoV1>}
     */
    public Pagination<ProjectV1> findProjectOrderByCtimeWithPage(Long loginUser, Integer pageNo, Integer pageSize, Boolean includeFinished, String queryKey) {

        //
        //BizAssert.authorized(this.permissionService.checkPermission(permissionConfig.getViewProject(), loginUser)
        //        , BizCodes.NO_PERMISSION);
        logger.info("[findProjectOrderByCtimeWithPage] Request coming: loginUser={}, pageNo={}, pageSize={}, includeFinished={}, queryKey={}",loginUser,pageNo,pageSize,includeFinished,queryKey);
        Sort sort = new Sort(Sort.Direction.DESC, "ctime");
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);

        //????????????????????????????????????ID???????????????????????????????????????????????????????????????????????????????????????????????????
//        List<Long> idList = permissionService.findAllIds(permissionConfig.getViewProject(), loginUser, permissionConfig.getProjectRuleFieldName());
        List<Long> idList = projectRepository.findAllIdsByCuser(loginUser);
        System.out.println("loginUser => "+loginUser+",idList => "+idList);
        Page<Project> projectPage;
        List<Project> projectList;
        Integer totalCount;

        /*
         *???????????????????????? ?????????????????????????????????
         */
        if (!queryKey.equals("")) {
            //?????????????????????????????????????????????
            if (includeFinished) {
                projectList = projectRepository.findAllByTitleStartingWithAndIdIn(queryKey, pageable, idList).getContent();

                //?????????????????????????????????????????????
                totalCount = projectRepository.countAllByTitleStartingWithAndIdIn(queryKey, idList);
            } else {
                projectList = projectRepository.findAllByStatusAndTitleStartingWithAndIdIn(Project.Status.NORMAL, queryKey, pageable, idList).getContent();
                totalCount = projectRepository.countAllByStatusAndTitleStartingWithAndIdIn(Project.Status.NORMAL, queryKey, idList);
            }
        }
        /*
         *????????????????????????????????????
         */
        else {
            if (includeFinished) {

                projectList = projectRepository.findAllByIdIn(pageable, idList).getContent();
                totalCount = projectRepository.countAllByIdIn(idList);
            } else {
                projectList = projectRepository.findAllByStatusAndIdIn(Project.Status.NORMAL, pageable, idList).getContent();
                totalCount = projectRepository.countAllByStatusAndIdIn(Project.Status.NORMAL, idList);
            }
        }
        //projectList.stream().map(l -> BeanUtils.convertType(l, ProjectV1.class)).collect(Collectors.toList());
        List<ProjectV1> projectV1List = projectList.stream().map(l -> BeanUtils.convertType(l, ProjectV1.class)).collect(Collectors.toList());

        Set userIdList = new HashSet();
        Map<Long, String> idNameMap = new HashMap<>();
        for (int i = 0; i < projectV1List.size(); i++) {
            Long id = projectV1List.get(i).getCuser();

            if (!userIdList.contains(id)) {
                userIdList.add(id);
                try {
                    String userName = this.userService.findUserNameById(id);
                    idNameMap.put(id, userName);
                } catch (Exception e) {
                    logger.error("[findProjectOrderByCtimeWithPage] ????????????username???Exception",e);
                    System.out.print(e);
                    throw e;
                }

            }

            ProjectV1 projectV1 = projectV1List.get(i);
            projectV1.setCuserName(idNameMap.get(projectV1.getCuser()));
            projectV1List.set(i, projectV1);
        }


        //??????VO????????? ?????????
        Pagination<ProjectV1> pagination = new Pagination<>();
        pagination.setTotalCount(totalCount);
        pagination.setPageNo(pageNo);
        pagination.setPageSize(pageSize);
        if (projectList.size() == 0) {
            pagination.setPageList(Collections.emptyList());
            return pagination;
        }
        pagination.setPageList(projectV1List);

        return pagination;
    }

    public ProjectBoV1 findProjectById(Long loginUser, Long projectId) {
//        BizAssert.authorized(this.permissionService.checkPermission(permissionConfig.getViewProject(), loginUser)
//                , BizCodes.NO_PERMISSION);
        logger.info("[findProjectById] Request coming: loginUser={}, projectId={}",loginUser,projectId);
        return BeanUtils.convertType(this.projectRepository.findById(projectId).orElse(null), ProjectBoV1.class);
    }

    /**
     * ????????????
     *
     * @param title       ????????????
     * @param description ????????????
     */
    public void createProjects(Long loginUser, String title, Long origanizationId, String description, String status) {

//        BizAssert.authorized(this.permissionService.checkPermission(permissionConfig.getCreateProject(), loginUser)
//                , BizCodes.NO_PERMISSION);

        logger.info("[createProject] Request coming: loginUser={}, title={}, origanizationId={}, description={}, status={}",loginUser,title,origanizationId,description,status);
        LocalDateTime ctime = LocalDateTime.now().withNano(0);
        LocalDateTime mtime = LocalDateTime.now().withNano(0);
        Project project = Project.builder()
                .title(title)
                .description(description)
                .cuser(loginUser)
                .muser(loginUser)
                .is_deleted(false)
                .organizationId(origanizationId)
                .status(Project.Status.NORMAL)
                .privateStatus(Project.PrivateStatus.valueOf(status))
                .ctime(ctime)
                .mtime(mtime)
                .build();
        this.projectRepository.saveAndFlush(project);
//         this.imageService.createProject(title, status, loginUser);
        //???permission???????????????????????????????????????????????????
//        this.permissionService.addData(permissionConfig.getProjectManagerAndProjectRuleId(), loginUser, project.getId(), loginUser);


    }
}
