<template>
    <div>
        <div>
            <div class="block_account mb20">
                <p class="block_title">
                    <img src="../../assets/account.svg"/>
                    <span>账户总览</span>
                </p>
                <div class="w pt60">
                    <div class="account_address">
                        <span>{{address}}</span>
                        <img class="csp" src="../../assets/copy.svg" v-clipboard:copy="address"
                             v-clipboard:success="copySuccess" v-clipboard:error="copyError"/>
                        <span>账户详情</span>
                    </div>
                    <p class="account_asset">资产：1,234,567,890 SS</p>
                    <div class="account_tool">
                        <button class="common_btn imgBtn">
                            <img src="../../assets/transfer.svg"/>
                            <!--<svg id="图层_1" data-name="图层 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 150 162.5"><defs><style>.cls-1{fill:#fff;}</style></defs><title>转账</title><path class="cls-1" d="M49,73.87H61.21v49.19a8.21,8.21,0,0,0,8.19,8.19h61.2a8.19,8.19,0,0,0,8.19-8.19h0V73.86H151a6.29,6.29,0,0,0,6.36-6.21,6.13,6.13,0,0,0-1.41-3.9,3.49,3.49,0,0,0-.66-0.73l-48.63-42a8.67,8.67,0,0,0-5.91-2.3,8.39,8.39,0,0,0-5.7,2.14L44.72,63a4.49,4.49,0,0,0-.79.87,6.13,6.13,0,0,0-1.32,3.8A6.3,6.3,0,0,0,49,73.87h0Z" transform="translate(-25 -18.75)"/><rect class="cls-1" y="150" width="150" height="12.5"/><rect class="cls-1" y="127.01" width="150" height="12.5"/></svg>-->
                            <span>转账</span>
                        </button>
                        <button class="common_btn imgBtn" @click="openSendMessageDialog">
                            <img src="../../assets/message.svg"/>
                            <span>发送消息</span>
                        </button>
                        <button class="common_btn imgBtn">
                            <img src="../../assets/setting.svg"/>
                            <span>HUB设置</span>
                        </button>
                    </div>
                </div>
            </div>
            <div class="block_receiptDisbursement mb20">
                <p class="block_title">
                    <img src="../../assets/receipt&disbursementInfo.svg"/>
                    <span>收支明细</span>
                </p>
                <div class="w">
                    <div class="whf">
                        此处为数据图表
                    </div>
                    <div class="whf">
                        此处为数据曲线
                    </div>
                </div>
            </div>
            <div class="block_list">
                <p  class="block_title">
                    <img src="../../assets/transaction.svg"/>
                    <span>交易记录</span>
                </p>
                <div class="list_table w br4">
                    <div class="list_content data_loading">
                        <table class="table table_striped" id="blocks_table">
                            <thead>
                            <tr>
                                <th>交易时间</th>
                                <th>区块高度</th>
                                <th>交易类型</th>
                                <th>金额</th>
                                <th>手续费</th>
                                <th class="dbw">交易账户</th>
                                <th>确认数量</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>2018/10/18 17:26:25</td>
                                <td class="linker">1234</td>
                                <td>普通支付</td>
                                <td>+10000000 SS</td>
                                <td>1 SS</td>
                                <td class="linker image_text w300">
                                    <span>SSA-9WKZ-DV7P-M6MN-5MH8B</span>
                                    <img src="../../assets/right_arrow.svg"/>
                                    <span>您</span>
                                </td>
                                <td>12323</td>
                                <td class="linker">查看详情</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination">
                        <div id="pagination_blocks" class="pagination"></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal" id="send_message_modal" v-show="sendMessage">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">发送信息</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item label="接受者">
                                <el-input v-model="messageForm.receiver"></el-input>
                            </el-form-item>
                            <el-form-item label="信息">
                                <el-checkbox v-model="messageForm.isEncrypted">加密信息</el-checkbox>
                                <el-input
                                    type="textarea"
                                    :autosize="{ minRows: 2, maxRows: 10}"
                                    resize="none"
                                    placeholder="请输入信息内容"
                                    v-model="messageForm.message">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="文件">
                                <el-input placeholder="请选择文件" class="input-with-select" v-model="messageForm.file">
                                    <el-button slot="append">浏览</el-button>
                                </el-input>
                                <input id="file" ref="file" type="file" @change="fileChange"/>
                            </el-form-item>
                            <el-form-item label="手续费">
                                <el-slider v-model="messageForm.fee" show-input :show-tooltip="false">
                                </el-slider>
                            </el-form-item>
                            <el-form-item label="秘钥">
                                <el-input v-model="messageForm.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">发送信息</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal" id="tranfer_accounts_modal" v-show="tranferAccounts">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">发送信息</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item label="接受者">
                                <el-input v-model="messageForm.receiver"></el-input>
                            </el-form-item>
                            <el-form-item label="信息">
                                <el-checkbox v-model="messageForm.isEncrypted">加密信息</el-checkbox>
                                <el-input
                                    type="textarea"
                                    :autosize="{ minRows: 2, maxRows: 10}"
                                    resize="none"
                                    placeholder="请输入信息内容"
                                    v-model="messageForm.message">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="文件">
                                <el-input placeholder="请选择文件" v-model="messageForm.file">
                                    <el-button slot="append">浏览</el-button>
                                </el-input>
                            </el-form-item>
                            <el-form-item label="手续费">
                                <el-slider v-model="messageForm.fee" show-input :show-tooltip="false">
                                </el-slider>
                            </el-form-item>
                            <el-form-item label="秘钥">
                                <el-input v-model="messageForm.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">发送信息</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    export default {
        name: "Network",
        components: {},
        data () {
            return {
                address: "SSA-9WKZ0DV7P-M6MN-5MH8B",
                sendMessage:false,
                tranferAccounts:false,

                messageForm:{
                    receiver:'1',
                    message:'2',
                    isEncrypted:true,
                    file:'',
                    fee:10,
                    password:''
                }
            };
        },
        methods:{
            openSendMessageDialog:function () {
                this.$store.state.mask = true;
                this.sendMessage = true;
            },
            closeDialog:function(){
                this.$store.state.mask = false;
                this.sendMessage = false;
            },
            copySuccess:function () {
                let _this = this;
                _this.$message({
                    showClose: true,
                    message: '已复制到剪切板',
                    type: 'success',
                });
            },
            copyError:function () {
                let _this = this;
                _this.$message({
                    showClose: true,
                    message: '复制失败',
                    type: 'error',

                });
            },

            fileChange:function (e) {

                let _this = this;
                _this.file = e.target.files[0].name;
                console.log("file",_this.file);

                _this.$message({
                    showClose: true,
                    message: _this.file,
                    type: 'success',
                });
            }
        }

    };
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
