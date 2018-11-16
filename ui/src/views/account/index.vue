<template>
    <div>
        <div class="block-account mb20">
            <p class="block-title">
                <img src="../../assets/account.png"/>
                <span>账户总览</span>
            </p>
            <div class="w pt60">
                <div class="account-address">
                    <span>SSA-9WKZ0DV7P-M6MN-5MH8B</span>
                    <img src="../../assets/copy.png"/>
                    <span>账户详情</span>
                </div>
                <p class="account-asset">资产：1,234,567,890 SS</p>
                <div class="account-tool">
                    <button class="common_btn"><img src="../../assets/transfer.png"/><span>转账</span></button>
                    <button class="common_btn"><img src="../../assets/message.png"/><span>发送消息</span></button>
                    <button class="common_btn"><img src="../../assets/setting.png"/><span>HUB设置</span></button>
                </div>
            </div>
        </div>
        <div class="block-receiptDisbursement mb20">
            <p class="block-title">
                <img src="../../assets/receipt&disbursementInfo.png"/>
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
        <div class="block-list">
            <p  class="block-title">
                <img src="../../assets/transaction.png"/>
                <span>交易记录</span>
            </p>
            <div class="list-table w br4">
                <div class="list-content data-loading">
                    <table class="table table-striped" id="blocks_table">
                        <thead>
                        <tr>
                            <th>交易时间</th>
                            <th>区块高度</th>
                            <th>交易类型</th>
                            <th>金额</th>
                            <th>手续费</th>
                            <th>交易账户</th>
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
                            <td class="linker transaction">
                                <span>SSA-9WKZ-DV7P-M6MN-5MH8B</span>
                                <img src="../../assets/right_arrow.png"/>
                                <span>您</span>
                            </td>
                            <td>12323</td>
                            <td class="linker">查看详情</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div class="list-pagination">
                    <div id="pagination_blocks" class="pagination"></div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "Network",
        components: {}
    }
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
