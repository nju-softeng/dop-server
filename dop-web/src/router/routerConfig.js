// 以下文件格式为描述路由的协议格式
// 你可以调整 routerConfig 里的内容


import BasicLayout from '../layouts/BasicLayout';
import {projectConfig} from '../pages/Projects'
import {pipelineConfig} from '../pages/Pipeline';
import NotFound from '../pages/NotFound';
import {loginConfig} from '../pages/Login'
import {permissionConfig} from "../pages/Permissions";
import codeConfig from "../pages/Code";
import {testConfig} from "../pages/TestCases";

const baseConfig = [
    {
        path: '*',
        layout: BasicLayout,
        component: NotFound,
    },
];


const routerConfig = [...testConfig, ...codeConfig, ...projectConfig,  ...permissionConfig, ...pipelineConfig, ...loginConfig, ...baseConfig];

export default routerConfig;
