<template>
    <div>
        <div>
            <div class="block_network mb20">
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
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.1.0</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker tl pl30">
                                        <el-tooltip class="item" content="API服务" placement="top" effect="light">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top" effect="light">
                                            <a>CS</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="商业API" placement="top" effect="light">
                                            <a>BI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="存储服务" placement="top" effect="light">
                                            <a>SE</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.0.1</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker tl pl30">
                                        <el-tooltip class="item" content="API服务" placement="top" effect="light">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top" effect="light">
                                            <a>CS</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.1.0</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker tl pl30">
                                        <el-tooltip class="item" content="API服务" placement="top" effect="light">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top" effect="light">
                                            <a>CS</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="商业API" placement="top" effect="light">
                                            <a>BI</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!--add black list-->
        <div class="modal" id="blacklist_peer_modal" v-show="blacklist">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">加入黑名单</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>是否将节点"114.115.210.116"添加到黑名单？</p>
                        <p>管理密码</p>
                        <input type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">添加</button>
                    </div>
                </div>
            </div>
        </div>

        <!--connect peer-->
        <div class="modal" id="connect_peer_modal" v-show="connectPeer">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">管理密码</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>节点名称：114.115.210.116</p>
                        <p>管理密码</p>
                        <input type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">连接</button>
                    </div>
                </div>
            </div>
        </div>

        <!--view peer info-->
        <div class="modal_info" id="peer_info" v-show="peerInfo">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >节点：114.115.210.116</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                        <tr>
                            <th>blockchain_state</th>
                            <td>UP_TO_DATE</td>
                            <th>通讯端口</th>
                            <td>8218</td>
                        </tr>
                        <tr>
                            <th>服务</th>
                            <td>API,CORS,BAPI,STORAGE</td>
                            <th>Outbound Web Socket</th>
                            <td>true</td>
                        </tr>
                        <tr>
                            <th>版本</th>
                            <td>COS 0.1.0</td>
                            <th>peer_load</th>
                            <td>[object Object]</td>
                        </tr>
                        <tr>
                            <th>平台</th>
                            <td>Linux amd64</td>
                            <th>Last Connection Attempt</th>
                            <td>2018/10/19 9:47:02</td>
                        </tr>
                        <tr>
                            <th>最后更新</th>
                            <td>2018/10/19 9:47:02</td>
                            <th>状态</th>
                            <td>CONNECTION</td>
                        </tr>
                        <tr>
                            <th>黑名单</th>
                            <td>false</td>
                            <th>共享地址</th>
                            <td>true</td>
                        </tr>
                        <tr>
                            <th>公布的地址</th>
                            <td>114.115.210.116</td>
                            <th>已下载</th>
                            <td>388 KB</td>
                        </tr>
                        <tr>
                            <th>Api Port</th>
                            <td>8215</td>
                            <th>已上传</th>
                            <td>988 KB</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

    </div>

</template>

<!--<span>是否将节点"114.115.210.116"添加到黑名单？</span>-->
            <!--<span>管理密码</span>-->
<script>
    export default {
        name: "Peers",
        components: {},
        data () {
            return {
                blacklist:false,
                connectPeer:false,
                peerInfo:false
            };
        },
        methods: {
            turn2network: function () {
                this.$router.push("/network");
            },
            openBlackDialog:function () {
                this.$store.state.mask = true;
                this.blacklist = true;
            },
            closeDialog:function(){
                this.$store.state.mask = false;
                this.blacklist = false;
                this.connectPeer = false;
                this.peerInfo = false
            },
            openConnectPeer:function () {
                this.$store.state.mask = true;
                this.connectPeer = true;
            },
            openInfo:function () {
                this.$store.state.mask = true;
                this.peerInfo = true;
            }
        },
        watch:{
            blacklist: function (val) {
                console.log("变了,"+val);
            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
