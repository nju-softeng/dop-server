package com.clsaa.dop.server.application.service;

import com.clsaa.dop.server.application.dao.AppEnvRepository;
import com.clsaa.dop.server.application.model.bo.AppEnvBoV1;
import com.clsaa.dop.server.application.model.bo.KubeCredentialBoV1;
import com.clsaa.dop.server.application.model.bo.KubeYamlDataBoV1;
import com.clsaa.dop.server.application.model.po.AppEnvironment;
import com.clsaa.dop.server.application.model.po.KubeCredential;
import com.clsaa.dop.server.application.model.po.KubeYamlData;
import com.clsaa.dop.server.application.util.BeanUtils;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.apis.CoreV1Api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service(value = "AppEnvService")
public class AppEnvService {
    @Autowired
    KubeYamlService kubeYamlService;

    @Autowired
    AppEnvRepository appEnvRepository;


    @Autowired
    KubeCredentialService kubeCredentialService;

    /**
     * 根据appId查询环境信息
     *
     * @param appID appID
     * @return{@link List<AppEnvBoV1> }
     */
    public List<AppEnvBoV1> findEnvironmentByAppId(Long appID) {
        return this.appEnvRepository.findAllByAppId(appID).stream().map(l -> BeanUtils.convertType(l, AppEnvBoV1.class)).collect(Collectors.toList());
    }

    /**
     * 创建环境信息
     *
     * @param appEnvironment appEnvironment
     * @return
     */
    public void createAppEnv(AppEnvironment appEnvironment) {
        this.appEnvRepository.saveAndFlush(appEnvironment);
    }


    /**
     * 根据ID查询环境详情信息
     *
     * @param id
     * @return AppEnvBoV1
     */
    public AppEnvBoV1 findEnvironmentDetailById(Long id) {
        AppEnvironment appEnvironment = this.appEnvRepository.findById(id).orElse(null);
        return BeanUtils.convertType(appEnvironment, AppEnvBoV1.class);
    }

    /**
     * 创建环境
     *
     * @param appId              appId
     * @param cuser              创建者
     * @param title              名称
     * @param environmentLever   环境级别
     * @param deploymentStrategy 发布策略
     */
    public void createEnvironmentByAppId(Long appId, Long cuser, String title, String environmentLever, String deploymentStrategy) {
        AppEnvironment appEnvironment = AppEnvironment.builder()
                .appId(appId)
                .title(title)
                .cuser(cuser)
                .muser(cuser)
                .ctime(LocalDateTime.now())
                .mtime(LocalDateTime.now())
                .environmentLevel(AppEnvironment.EnvironmentLevel.valueOf(environmentLever))
                .deploymentStrategy(AppEnvironment.DeploymentStrategy.valueOf(deploymentStrategy))
                .build();
        this.appEnvRepository.saveAndFlush(appEnvironment);
        if (deploymentStrategy.equals("KUBERNETES")) {
            this.kubeCredentialService.createCredentialByAppEnvId(cuser, appEnvironment.getId());
        }

    }

    /**
     * 根据ID删除环境信息
     *
     * @param id appId
     */
    public void deleteEnvironmentById(Long id) {
        this.appEnvRepository.deleteById(id);
    }


    /**
     * 获取该应用对应的cluster的所有命名空间
     *
     * @param id 应用环境id
     * @renturn{@link List<String>}
     */
    public List<String> findNameSpaces(Long id) throws Exception {
        CoreV1Api api = getCoreApi(id);

        return api.listNamespace(true, null, null, null, null, null, null, null, false)
                .getItems()
                .stream()
                .map(v1Namespace -> v1Namespace.getMetadata().getName())
                .collect(Collectors.toList());

    }


    /**
     * 根据id获取client
     *
     * @param id 应用环境id
     * @renturn ApiClient
     */
    public ApiClient getClient(Long id) {
        KubeCredentialBoV1 kubeCredentialBoV1 = this.kubeCredentialService.findByAppEnvId(id);
        String url = kubeCredentialBoV1.getTargetClusterUrl();
        String token = kubeCredentialBoV1.getTargetClusterToken();
        ApiClient client = Config.fromToken(url,
                token,
                false);
        return client;

    }

    /**
     * 根据id获取api
     *
     * @param id 应用环境id
     * @renturn CoreV1Api
     */
    public CoreV1Api getCoreApi(Long id) {

        return new CoreV1Api(getClient(id));
    }


    /**
     * 根据id获取api
     *
     * @param id 应用环境id
     * @renturn AppsV1Api
     */
    public AppsV1Api getAppsApi(Long id) {
        getClient(id);
        return new AppsV1Api(getClient(id));
    }


    /**
     * 根据命名空间获取服务列表
     *
     * @param id        应用环境id
     * @param namespace 命名空间
     * @renturn{@link List<String>}
     */
    public List<String> getServiceByNameSpace(Long id, String namespace) throws Exception {
        CoreV1Api api = getCoreApi(id);

        return api.listNamespacedService(namespace, false, null, null, null, null, Integer.MAX_VALUE, null, null, false)
                .getItems()
                .stream()
                .map(v1Service -> v1Service.getMetadata().getName())
                .collect(Collectors.toList());
    }


    /**
     * 更新url和token信息
     *
     * @param appEnvId    应用环境id
     * @param url   url
     * @param token token
     */
    public void updateUrlAndToken(Long muser, Long appEnvId, String url, String token) {
        this.kubeCredentialService.updateClusterInfo(muser, appEnvId, url, token);
    }


    /**
     * 根据命名空间及服务名称获取部署
     *
     * @param id        应用环境id
     * @param namespace 命名空间
     * @param service   服务
     * @renturn{@link List<String>}
     */
    public HashMap<String, Object> getDeploymentByNameSpaceAndService(Long id, String namespace, String service) throws Exception {

        AppsV1Api api = getAppsApi(id);


        V1DeploymentList deploymentList = api.listNamespacedDeployment(namespace, false, null, null, null, "app=" + service, Integer.MAX_VALUE, null, null, false);

        List<V1Deployment> v1DeploymentList = deploymentList.getItems();

        List<String> nameList = v1DeploymentList.stream().map(v1Deployment -> v1Deployment.getMetadata().getName()).collect(Collectors.toList());
        Map<String, List<String>> containerList = new HashMap<>();
        List<List<String>> lists = deploymentList.getItems().stream().map(
                v1Deployment -> v1Deployment.getSpec().getTemplate().getSpec().getContainers().stream().map(
                        v1Container -> v1Container.getName()).collect(Collectors.toList()
                )).collect(Collectors.toList());

        for (int i = 0; i < nameList.size(); i++) {
            containerList.put(nameList.get(i), v1DeploymentList.get(i).getSpec().getTemplate().getSpec().getContainers().stream().map(
                    v1Container -> v1Container.getName()).collect(Collectors.toList()
            ));
        }


        return new HashMap<String, Object>() {
            {
                put("deployment", nameList);
                put("containers", containerList);
            }

        };
    }


    /**
     * 创建服务
     *
     * @param id        应用环境id
     * @param namespace 命名空间
     * @param name      服务
     * @param port      容器
     */
    public void createServiceByNameSpace(Long id, String namespace, String name, Integer port) throws Exception {
        CoreV1Api coreApi = getCoreApi(id);

        AppsV1Api appsV1Api = getAppsApi(id);
        //V1Deployment deployment = new V1DeploymentBuilder()
        //        .withNewMetadata()
        //        .withName("test-deployment2")
        //        .addToLabels("app", name)
        //        .endMetadata()
        //        .withNewSpec()
        //        .withReplicas(2)
        //        .withNewSelector()
        //        .addToMatchLabels("app",name)
        //        .endSelector()
        //        .withNewTemplate()
        //        .withNewMetadata()
        //        .addToLabels("app", name)
        //        .endMetadata()
        //        .withNewSpec()
        //        .addNewContainer()
        //        .withName(name)
        //        .withImage( "registry.dop.clsaa.com/dop/dop-web:3")
        //        .endContainer()
        //        .addNewContainer()
        //        .withName("test-container")
        //        .withImage( "registry.dop.clsaa.com/dop/dop-web:4")
        //        .endContainer()
        //        .endSpec()
        //        .endTemplate()
        //        .endSpec()
        //        .build();
        //
        //V1Deployment deployment2 = new V1DeploymentBuilder()
        //        .withNewMetadata()
        //        .withName("test-deployment")
        //        .addToLabels("app", name)
        //        .endMetadata()
        //        .withNewSpec()
        //        .withReplicas(2)
        //        .withNewSelector()
        //        .addToMatchLabels("app",name)
        //        .endSelector()
        //        .withNewTemplate()
        //        .withNewMetadata()
        //        .addToLabels("app", name)
        //        .endMetadata()
        //        .withNewSpec()
        //        .addNewContainer()
        //        .withName(name)
        //        .withImage("registry.dop.clsaa.com/dop/dop-web:5")
        //        .endContainer()
        //        .endSpec()
        //        .endTemplate()
        //        .endSpec()
        //        .build();
        //
        //V1ReplicationController replicationController =
        //        new V1ReplicationControllerBuilder()
        //                .withNewMetadata()
        //                .withName(name)
        //                .endMetadata()
        //                .withNewSpec()
        //                .withReplicas(replicas.intValue())
        //                .addToSelector("app", name)
        //                .withNewTemplate()
        //                .withNewMetadata()
        //                .withName(name)
        //                .addToLabels("app", name)
        //                .endMetadata()
        //                .withNewSpec()
        //                .addNewContainer()
        //                .withName(name)
        //                .withImage(image)
        //                .addNewPort()
        //                .withContainerPort(port.intValue())
        //                .endPort()
        //                .endContainer()
        //                .endSpec()
        //                .endTemplate()
        //                .endSpec()
        //                .build();


        V1Service service =
                new V1ServiceBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .addToLabels("app", name)
                        .endMetadata()
                        .withNewSpec()
                        .addNewPort()
                        .withProtocol("TCP")
                        .withPort(port)
                        .endPort()
                        .addToSelector("app", name)
                        .endSpec()
                        .build();
        //api.createNamespacedReplicationController(namespace,replicationController,null,null,null);
        //     appsV1Api.createNamespacedDeployment(namespace,deployment,false,null,null);
        //appsV1Api.createNamespacedDeployment(namespace,deployment2,false,null,null);
        coreApi.createNamespacedService(namespace, service, false, null, null);

    }

    /**
     * 创建YAML信息
     *
     * @param appEnvId        应用环境id
     * @param cuser           创建者
     * @param nameSpace       命名空间
     * @param service         服务
     * @param deployment      部署
     * @param containers      容器
     * @param releaseStrategy 发布策略
     * @param releaseBatch    发布批次
     * @param replicas        副本数量
     */
    public void CreateYamlInfoByAppEnvId(Long appEnvId, Long cuser, String nameSpace, String service, String deployment, String containers, String releaseStrategy, Integer replicas
            , Long releaseBatch, String imageUrl, String yamlFilePath) throws Exception {
        this.kubeYamlService.CreateYamlData(appEnvId, cuser, nameSpace, service, deployment, containers, releaseStrategy, replicas
                , releaseBatch, imageUrl, yamlFilePath);

    }

    /**
     * 更新YAML信息
     *
     * @param appEnvId        应用环境id
     * @param cuser           创建者
     * @param nameSpace       命名空间
     * @param service         服务
     * @param deployment      部署
     * @param containers      容器
     * @param releaseStrategy 发布策略
     * @param releaseBatch    发布批次
     * @param replicas        副本数量
     */
    public void UpdateYamlInfoByAppEnvId(Long appEnvId, Long cuser, String nameSpace, String service, String deployment, String containers, String releaseStrategy, Integer replicas
            , Long releaseBatch, String imageUrl, String yamlFilePath) throws Exception {
        this.kubeYamlService.UpdateYamlData(appEnvId, cuser, nameSpace, service, deployment, containers, releaseStrategy, replicas
                , releaseBatch, imageUrl, yamlFilePath);

    }


    public HashMap<String, String> createYamlFileForDeploy(Long appEnvId) throws Exception {

        KubeYamlDataBoV1 kubeYamlDataBoV1 = this.kubeYamlService.findYamlDataByEnvId(appEnvId);
        if (kubeYamlDataBoV1.getYamlFilePath() == "") {
            return new HashMap<String, String>() {{
                put("path", kubeYamlDataBoV1.getYamlFilePath());
            }};
        } else {
            return new HashMap<String, String>() {{
                put("yaml", kubeYamlDataBoV1.getDeploymentEditableYaml());
            }};
        }


    }

}
