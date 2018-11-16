<template>
    <div>
        <div class="block_network mb10">
            <p class="block_title csp">
                <a @click="turn2network">
                    <span>&lt;&lt;返回网络</span>
                </a>
            </p>
            <div class="w dfl">
                <div class="block_blue radius_blue">
                    <p>在线节点数</p>
                    <p><span></span>个</p>
                </div>
                <div class="block_blue radius_blue">
                    <p>HUB运行数</p>
                    <p><span></span>个</p>
                </div>
                <div class="block_blue radius_blue">
                    <p>活跃节点数</p>
                    <p><span></span>个</p>
                </div>
            </div>
        </div>
        <div class="block_map">

        </div>
        <div class="block_list">
            <p class="block_title">
                <img src="../../assets/peerlist.svg"/>
                <span>节点列表</span>
            </p>
            <div class="list_table w br4">
                <div class="list_content table_responsive data-loading">
                    <table class="table table-striped"  id="peers_table">
                        <thead>
                            <tr>
                                <th>节点地址</th>
                                <th>已下载</th>
                                <th>已上传</th>
                                <th>应用程序</th>
                                <th>平台</th>
                                <th>服务</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="image_text">
                                    <img src="../../assets/success.svg"/>
                                    <span>114.115.210.116</span>
                                </td>
                                <td>113KB</td>
                                <td>133KB</td>
                                <td><span class="patch">COS0.1.0</span></td>
                                <td>Linux amd64</td>
                                <td class="linker">
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>AI</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>CS</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>BI</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="存储服务" placement="top">
                                        <a>SE</a>
                                    </el-tooltip>
                                </td>
                                <td>
                                    <button class="list_button w40">连接</button>
                                    <button class="list_button w50">黑名单</button>
                                </td>
                            </tr>
                            <tr>
                                <td class="image_text">
                                    <img src="../../assets/error.svg"/>
                                    <span>114.115.210.117</span>
                                </td>
                                <td>113KB</td>
                                <td>133KB</td>
                                <td><span class="patch">COS0.0.1</span></td>
                                <td>Linux amd64</td>
                                <td class="linker">
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>AI</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>CS</a>
                                    </el-tooltip>
                                </td>
                                <td>
                                    <button class="list_button w40">连接</button>
                                    <button class="list_button w50">黑名单</button>
                                </td>
                            </tr>
                            <tr>
                                <td class="image_text">
                                    <img src="../../assets/success.svg"/>
                                    <span>114.115.210.116</span>
                                </td>
                                <td>113KB</td>
                                <td>133KB</td>
                                <td><span class="patch">COS0.1.0</span></td>
                                <td>Linux amd64</td>
                                <td class="linker tl">
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>AI</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>CS</a>
                                    </el-tooltip>
                                    <el-tooltip class="item" content="API服务" placement="top">
                                        <a>BI</a>
                                    </el-tooltip>
                                </td>
                                <td>
                                    <button class="list_button w40">连接</button>
                                    <button class="list_button w50">黑名单</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>


    <!--blacklist-->
    <!--<el-dialog
        title="加入黑名单"
        :visible.sync="blacklistDialog"
        width="30%"
        center>
        <span>是否将节点"114.115.210.116"添加到黑名单？</span>
        <span>管理密码</span>
        <input/>
        <span slot="footer" class="dialog-footer">
            <el-button type="primary" @click="blacklistDialog = false">添加</el-button>
        </span>
    </el-dialog>-->
</template>
<!--<span>是否将节点"114.115.210.116"添加到黑名单？</span>
            <span>管理密码</span>-->
<script>
    export default {
        name: "Peers",
        components: {},
        data () {
            return {
                blacklist:false
            };
        },
        methods: {
            turn2network: function () {
                this.$router.push("/network");
            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
