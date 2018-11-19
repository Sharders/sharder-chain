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
                        <img class="csp" src="../../assets/copy.svg" v-clipboard:copy="address"/>
                        <span>账户详情</span>
                    </div>
                    <p class="account_asset">资产：1,234,567,890 SS</p>
                    <div class="account_tool">
                        <button class="common_btn"><img src="../../assets/transfer.svg"/><span>转账</span></button>
                        <button class="common_btn" @click="openSendMessageDialog"><img src="../../assets/message.svg"/><span>发送消息</span></button>
                        <button class="common_btn"><img src="../../assets/setting.svg"/><span>HUB设置</span></button>
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
                                <td class="linker image_text">
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
    </div>
</template>
<script>
    export default {
        name: "Network",
        components: {},
        data () {
            return {
                address: "SSA-9WKZ0DV7P-M6MN-5MH8B",
                sendMessage:false
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
        }

    };
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
