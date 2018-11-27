<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
    <div>
        <div>
            <div class="block_account mb20">
                <p class="block_title">
                    <img src="../../assets/account.svg"/>
                    <span>账户总览</span>
                </p>
                <div class="w pt60">
                    <div class="account_address">
                        <span>{{accountInfo.accountRS}}</span>
                        <img class="csp" src="../../assets/copy.svg" v-clipboard:copy="accountInfo.accountRS"
                             v-clipboard:success="copySuccess" v-clipboard:error="copyError"/>
                        <span class="csp" @click="openAccountInfoDialog">账户详情</span>
                    </div>
                    <p class="account_asset">资产：{{$global.formatMoney(accountInfo.balanceNQT/100000000)}} SS</p>
                    <div class="account_tool">
                        <button class="common_btn imgBtn" @click="openTransferDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 150 162.5">
                                    <path d="M49,73.87H61.21v49.19a8.21,8.21,0,0,0,8.19,8.19h61.2a8.19,8.19,0,0,0,8.19-8.19h0V73.86H151a6.29,6.29,0,0,0,6.36-6.21,6.13,6.13,0,0,0-1.41-3.9,3.49,3.49,0,0,0-.66-0.73l-48.63-42a8.67,8.67,0,0,0-5.91-2.3,8.39,8.39,0,0,0-5.7,2.14L44.72,63a4.49,4.49,0,0,0-.79.87,6.13,6.13,0,0,0-1.32,3.8A6.3,6.3,0,0,0,49,73.87h0Z" transform="translate(-25 -18.75)"/>
                                    <rect y="150" width="150" height="12.5"/>
                                    <rect y="127.01" width="150" height="12.5"/>
                                </svg>
                            </span>
                            <span>转账</span>
                        </button>
                        <button class="common_btn imgBtn" @click="openSendMessageDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 171.43 137.08">
                                    <path class="cls-1" d="M173.41,143.1a10.26,10.26,0,0,1-10.24,10.25H36.84A10.26,10.26,0,0,1,26.59,143.1v-92A10.25,10.25,0,0,1,36.84,40.88H163.16a10.25,10.25,0,0,1,10.24,10.24v92ZM163.16,28.57H36.84A22.57,22.57,0,0,0,14.29,51.12v92a22.57,22.57,0,0,0,22.55,22.55H163.16a22.57,22.57,0,0,0,22.55-22.55v-92A22.57,22.57,0,0,0,163.16,28.57ZM151.88,65L100,94.89,47.26,65a6.31,6.31,0,0,0-8.39,2.31,6.16,6.16,0,0,0,2.31,8.39L100,109.07l58-33.44a6.16,6.16,0,0,0,2.26-8.41,6.33,6.33,0,0,0-8.4-2.25h0Z" transform="translate(-14.29 -28.57)"/>
                                </svg>
                            </span>
                            <span>发送消息</span>
                        </button>
                        <button class="common_btn imgBtn" @click="openHubSettingDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 191.64 181.04">
                                    <path d="M-382,127.83h0v0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-210.23,147l-0.07,0h0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-363.68,147h-0.08Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-227.43,189.31l-0.09.08,0,0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-296.8,31l0,0h0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-210.36,81h0Z" transform="translate(382.82 -23.48)"/>
                                </svg>
                            </span>
                            <span v-if="!$store.state.isPassphrase">HUB设置</span>
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
                    <div class="whf" id="transaction_amount_bar">
                        此处为数据图表
                    </div>
                    <div class="whf" id="yield_curve">
                        此处为数据曲线
                    </div>
                </div>
            </div>
            <div class="block_list">
                <p  class="block_title fl">
                    <img src="../../assets/transaction.svg"/>
                    <span>交易记录</span>
                </p>
                <div class="transaction_type">
                    <el-select v-model="selectType" placeholder="全部">
                        <el-option
                            v-for="item in transactionType"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                </div>
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
                                <th class="dbw w300">交易账户</th>
                                <th>确认数量</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(transaction,index) in accountTransactionList" v-if="index>=0 && index <= 9">
                                <td>{{$global.myFormatTime(transaction.timestamp, 'YMDHMS')}}</td>
                                <td class="linker">{{transaction.height}}</td>
                                <td>{{transaction.type}}</td>
                                <td>+{{$global.formatMoney(transaction.amountNQT/100000000)}} SS</td>
                                <td>{{$global.formatMoney(transaction.feeNQT/100000000)}} SS</td>
                                <td class=" image_text w300">
                                    <span class="linker" @click="accountInfoDialog = true" v-if="transaction.type === 9">Coinbase</span>
                                    <img src="../../assets/right_arrow.svg"/>
                                    <span class="linker" @click="accountInfoDialog = true">您</span> <!--transaction.senderRS-->
                                </td>
                                <td>{{transaction.confirmations}}</td>
                                <td class="linker" @click="openAccountInfoDialog">查看详情</td>
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
        <!--view send message dialog-->
        <div class="modal" id="send_message_modal" v-show="sendMessageDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">发送信息</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item label="接收者">
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
        <!--view tranfer account dialog-->
        <div class="modal" id="tranfer_accounts_modal" v-show="tranferAccountsDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">发送信息</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item label="接收者">
                                <el-input v-model="transfer.receiver"></el-input>
                            </el-form-item>
                            <el-form-item label="数额">
                                <el-input v-model="transfer.number"></el-input>
                                <label class="input_suffix">SS</label>
                            </el-form-item>
                            <el-form-item label="手续费">
                                <el-slider v-model="messageForm.fee" show-input :show-tooltip="false">
                                </el-slider>
                            </el-form-item>
                            <el-form-item label="">
                                <el-checkbox v-model="transfer.hasMessage">添加一条信息</el-checkbox>
                                <el-checkbox ref="encrypted2" v-model="transfer.isEncrypted" :disabled="ncryptedDisabled">加密信息</el-checkbox>
                                <el-input
                                    type="textarea"
                                    :autosize="{ minRows: 2, maxRows: 10}"
                                    resize="none"
                                    placeholder="请输入信息内容"
                                    v-model="transfer.message">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="秘钥">
                                <el-input v-model="transfer.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">发送</button>
                    </div>
                </div>
            </div>
        </div>
        <!--view tranfer account dialog-->
        <div class="modal_hubSetting" id="hub_setting" v-show="hubSettingDialog">
            <div class="modal-header">
                <button class="common_btn">重置Hub</button>
                <button class="common_btn">重启Hub</button>
                <h4 class="modal-title">
                    <span>Hub设置</span>
                </h4>

            </div>
            <div class="modal-body">
                <div class="version_info">
                    <span>当前版本：</span>
                    <span>0.1.0</span>
                    <span>发现新版本</span>
                    <span>点击更新</span>
                </div>
                <el-form label-position="left" label-width="160px">
                    <el-form-item label="启动内网穿透服务:">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item label="Sharder官网账户:">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item label="Sharder官网密码:">
                        <el-input v-model="hubsetting.sharderPwd" :disabled="punchthroughDisabled"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务地址:">
                        <el-input v-model="hubsetting.address" :disabled="punchthroughDisabled"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务端口:">
                        <el-input v-model="hubsetting.port" :disabled="punchthroughDisabled"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务客户端秘钥:">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="punchthroughDisabled"></el-input>
                    </el-form-item>
                    <el-form-item label="公网地址:">
                        <el-input v-model="hubsetting.publicAddress" :disabled="punchthroughDisabled"></el-input>
                    </el-form-item>
                    <el-form-item label="关联SS地址:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item label="是否开启挖矿:">
                        <el-checkbox v-model="hubsetting.isOpenMining">锻造以启用</el-checkbox>
                    </el-form-item>
                    <el-form-item label="改绑助记词:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item label="新密码:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item label="确认新密码:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn">确认</button>
                    <button class="common_btn" @click="closeDialog()">取消</button>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="trading_info" v-show="tradingInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >交易详情</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                    <tr><th>签名</th>
                        <td>4342343j4hj343b3i4hgb3hb43jkbi34gh3b43j4n32i4gh32i4b324b33
                        334j3h4nj3k4b3jk4b3kj4b34kj3b4jk3b43kj4b3k4b3k4jb3k4j3b4kj3b4
                        4343jk4b3kj4b3kj4b3kj4b3k4b3k4b343</td></tr>
                    <tr>
                        <th>交易</th><td>0</td></tr>
                    <tr>
                        <th>类型</th><td>0</td></tr>
                    <tr>
                        <th>哈希签名</th><td>234k3n43lk4n3ln4jk32nh4jikbh4k3j2b4kj324bn32kj4b232bk23b3b3kj</td></tr>
                    <tr>
                        <th>发送者</th><td>SSA-9W2N-D8F2-M9DN-2JD4F</td></tr>
                    <tr>
                        <th>数额</th><td>1,000,000 SS</td></tr>
                    <tr>
                        <th>接收者</th><td>您</td></tr>
                    <tr>
                        <th>区块时间戳</th><td>67943707 | 2018/10/11 17:21:24</td></tr>
                    <tr>
                        <th>时间戳</th><td>67943707 | 2018/10/11 17:21:24</td></tr>
                    <tr>
                        <th>发送者公钥</th><td></td></tr>
                    <tr>
                        <th>手续费</th><td>1 SS</td></tr>
                    <tr>
                        <th>确认</th><td>12345</td></tr>
                    <tr>
                        <th>类型完整哈希：</th><td>234k3n43lk4n3ln4jk32nh4jikbh4k3j2b4kj324bn32kj4b232bk23b3b3kj</td></tr>
                    <tr>
                        <th>版本：</th><td>1</td></tr>
                    <tr>
                        <th>发送者</th><td>21321312122121213</td></tr>
                    <tr>
                        <th>接收者</th><td>您</td></tr>
                    <tr>
                        <th>区块高度</th><td>12345</td></tr>
                    </tbody>
                </table>
            </div>

        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="account_info" v-show="accountInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户详情</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                    <tr>
                        <th>账户地址:</th>
                        <td>{{accountInfo.accountRS}}</td></tr>
                    <tr>
                        <th>账户名：</th>
                        <td>
                            <div class="accountName" v-if="isShowName">
                                <span>{{accountInfo.name}}</span><img src="../../assets/rewrite.svg" @click="isShowName = false"/>
                            </div>
                            <div class="rewriteName" v-else>
                                <el-input v-model="messageForm.receiver"></el-input>
                                <button class="common_btn" @click="isShowName = true">确认</button>
                            </div>
                        </td></tr>
                    <tr>
                        <th>账户余额：</th><td>{{$global.formatMoney(accountInfo.balanceNQT/100000000)}} SS</td></tr>
                    <tr>
                        <th>可用余额：</th><td>{{$global.formatMoney(accountInfo.effectiveBalanceSS)}} SS</td></tr>
                    <tr>
                        <th>挖矿余额：</th><td>{{$global.formatMoney(accountInfo.forgedBalanceNQT/100000000)}} SS</td></tr>
                    <tr>
                        <th>公钥</th><td>{{accountInfo.publicKey}}</td></tr>
                    </tbody>
                </table>
            </div>

        </div>

    </div>
</template>
<script>
    import echarts from "echarts";
    export default {
        name: "Network",
        components: {echarts},
        data () {
            return {
                //dialog
                sendMessageDialog: false,
                tranferAccountsDialog: false,
                hubSettingDialog:false,
                tradingInfoDialog:false,
                accountInfoDialog:false,

                ncryptedDisabled:true,
                punchthroughDisabled:true,
                isShowName:true,

                messageForm: {
                    receiver: "1",
                    message: "2",
                    isEncrypted: false,
                    file: "",
                    fee: 0,
                    password: ""
                },
                transfer: {
                    receiver: "1",
                    number:0,
                    fee:0,
                    hasMessage:false,
                    message:"",
                    isEncrypted:false,
                    password:""
                },
                hubsetting:{
                    openPunchthrough:false,
                    sharderAccount:'',
                    sharderPwd:'',
                    address:'',
                    port:'',
                    clientSecretkey:'',
                    publicAddress:'',
                    SS_Address:'',
                    isOpenMining:false,
                    modifyMnemonicWord:'',
                    newPwd:'',
                    confirmPwd:''
                },
                accountInfo:{
                    accountRS:'SSA-J2TM-GKMD-KTER-27GF7',
                    publicKey:'',
                    accountId:'',
                    forgedBalanceNQT:0,      //挖矿余额
                    effectiveBalanceSS:0,   //可用余额
                    guaranteedBalanceNQT:0,  //保证余额
                    balanceNQT:0,            //账户余额
                    name:'',
                    description:'',
                },
                selectType:'',
                transactionType:[{
                    value:'',
                    label:'全部'
                },{
                    value:0,
                    label:'转账'
                },{
                    value:6,
                    label:'存储'
                },{
                    value:9,
                    label:'出块奖励'
                }],

                accountTransactionList:[]
            };
        },
        created:function(){
            const _this = this;
            this.$http.get('/sharder?requestType=getAccount',{
                params:{
                    account:_this.accountInfo.accountRS,
                    includeLessors:true,
                    includeAssets:true,
                    includeEffectiveBalance:true,
                    includeCurrencies:true,

                }
            }).then(function (res) {
                _this.accountInfo = res.data;
                console.log(_this.accountInfo);
            }).catch(function (err) {
                console.log(err);
            });

            _this.getAccountTransactionList();
        },
        methods: {
            drawBarchart: function () {
                const barchart = echarts.init(document.getElementById("transaction_amount_bar"));

                const option = {
                    grid: {
                        left: '5%',
                        right: '2%',
                        top: '10%',
                        bottom: '15%',
                    },
                    xAxis: {
                        type: 'category',
                        data: ['待合并', '待合并', '待合并', '待合并', '待合并']
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [{
                        data: [34, 12, 60, 10, 90],
                        type: 'bar'
                    }]
                };
                if (option && typeof option === "object") {
                    barchart.setOption(option, true);
                }
            },
            drawYield:function(){
                const yieldCurve = echarts.init(document.getElementById("yield_curve"));

                const option = {
                    grid: {
                        left: '5%',
                        right: '2%',
                        top: '10%',
                        bottom: '15%',
                    },
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
                        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [{
                        data: [820, 932, 901, 934, 1290, 1330, 1320],
                        type: 'line',
                        smooth: true
                    }]
                };
                if (option && typeof option === "object") {
                    yieldCurve.setOption(option, true);
                }
            },
            getAccountTransactionList:function(){
                const _this = this;
                this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                    params:{
                        account:_this.accountInfo.accountRS,
                        type:_this.selectType
                    }
                }).then(function (res) {
                    _this.accountTransactionList = res.data.transactions;
                    console.log(_this.accountTransactionList);
                }).catch(function (err) {
                    console.log(err);
                });
            },

            openSendMessageDialog: function () {
                this.$store.state.mask = true;
                this.sendMessageDialog = true;
            },
            openTransferDialog:function(){
                this.$store.state.mask = true;
                this.tranferAccountsDialog = true;
            },
            openHubSettingDialog:function(){
                this.$store.state.mask = true;
                this.hubSettingDialog = true;
            },
            openTradingInfoDialog:function(){
                this.$store.state.mask = true;
                this.tradingInfoDialog = true;
            },
            openAccountInfoDialog:function(){
                this.$store.state.mask = true;
                this.accountInfoDialog = true;
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.sendMessageDialog = false;
                this.tranferAccountsDialog = false;
                this.hubSettingDialog = false;
                this.tradingInfoDialog = false;
                this.accountInfoDialog = false;
            },
            copySuccess: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: "已复制到剪切板",
                    type: "success"
                });
            },
            copyError: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: "复制失败",
                    type: "error"

                });
            },

            fileChange: function (e) {
                const _this = this;
                _this.file = e.target.files[0].name;
                console.log("file", _this.file);

                _this.$message({
                    showClose: true,
                    message: _this.file,
                    type: "success"
                });
            }
        },
        watch:{
            transfer:{
                handler(val, oldVal){
                    const _this = this;
                    if(_this.transfer.hasMessage){
                        _this.ncryptedDisabled = false;
                    }else{
                        _this.ncryptedDisabled = true;
                        _this.transfer.isEncrypted = false;
                    }
                },
                deep: true
            },
            hubsetting:{
                handler(val, oldVal){
                    const _this = this;
                    if(_this.hubsetting.openPunchthrough){
                        _this.punchthroughDisabled = false;
                    }else{
                        _this.punchthroughDisabled = true;
                        _this.hubsetting.sharderPwd = '';
                        _this.hubsetting.address = '';
                        _this.hubsetting.port = '';
                        _this.hubsetting.clientSecretkey = '';
                        _this.hubsetting.publicAddress = '';

                    }
                },
                deep: true
            }
        },
        mounted () {
            this.drawBarchart();
            this.drawYield();
        },
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped  lang="scss" type="text/scss">
    .el-select-dropdown{
        .el-select-dropdown__item.selected{
            background-color: #493eda!important;
            color: #fff!important;
        }
        .el-select-dropdown__item.selected.hover{
            background-color: #493eda!important;
            color: #fff!important;
        }
    }
</style>
