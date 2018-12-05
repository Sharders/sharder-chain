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
                        <span class="csp" @click="openUserInfoDialog">账户详情</span>
                    </div>
                    <p class="account_asset">资产：{{$global.formatMoney(accountInfo.balanceNQT/100000000)}} SS</p>
                    <div class="account_tool">
                        <button class="common_btn imgBtn" @click="openTransferDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 150 162.5">
                                    <path
                                        d="M49,73.87H61.21v49.19a8.21,8.21,0,0,0,8.19,8.19h61.2a8.19,8.19,0,0,0,8.19-8.19h0V73.86H151a6.29,6.29,0,0,0,6.36-6.21,6.13,6.13,0,0,0-1.41-3.9,3.49,3.49,0,0,0-.66-0.73l-48.63-42a8.67,8.67,0,0,0-5.91-2.3,8.39,8.39,0,0,0-5.7,2.14L44.72,63a4.49,4.49,0,0,0-.79.87,6.13,6.13,0,0,0-1.32,3.8A6.3,6.3,0,0,0,49,73.87h0Z"
                                        transform="translate(-25 -18.75)"/>
                                    <rect y="150" width="150" height="12.5"/>
                                    <rect y="127.01" width="150" height="12.5"/>
                                </svg>
                            </span>
                            <span>转账</span>
                        </button>
                        <button class="common_btn imgBtn" @click="openSendMessageDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 171.43 137.08">
                                    <path class="cls-1"
                                          d="M173.41,143.1a10.26,10.26,0,0,1-10.24,10.25H36.84A10.26,10.26,0,0,1,26.59,143.1v-92A10.25,10.25,0,0,1,36.84,40.88H163.16a10.25,10.25,0,0,1,10.24,10.24v92ZM163.16,28.57H36.84A22.57,22.57,0,0,0,14.29,51.12v92a22.57,22.57,0,0,0,22.55,22.55H163.16a22.57,22.57,0,0,0,22.55-22.55v-92A22.57,22.57,0,0,0,163.16,28.57ZM151.88,65L100,94.89,47.26,65a6.31,6.31,0,0,0-8.39,2.31,6.16,6.16,0,0,0,2.31,8.39L100,109.07l58-33.44a6.16,6.16,0,0,0,2.26-8.41,6.33,6.33,0,0,0-8.4-2.25h0Z"
                                          transform="translate(-14.29 -28.57)"/>
                                </svg>
                            </span>
                            <span>发送消息</span>
                        </button>
                        <button class="common_btn imgBtn" v-if="typeof(secretPhrase) !== 'undefined'" @click="openHubSettingDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 191.64 181.04">
                                    <path d="M-382,127.83h0v0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-210.23,147l-0.07,0h0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-363.68,147h-0.08Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-227.43,189.31l-0.09.08,0,0Z" transform="translate(382.82 -23.48)"/>
                                    <path
                                        d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z"
                                        transform="translate(382.82 -23.48)"/>
                                    <path
                                        d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z"
                                        transform="translate(382.82 -23.48)"/>
                                    <path d="M-296.8,31l0,0h0Z" transform="translate(382.82 -23.48)"/>
                                    <path d="M-210.36,81h0Z" transform="translate(382.82 -23.48)"/>
                                </svg>
                            </span>
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
                    <div class="whf" id="transaction_amount_bar">
                        此处为数据图表
                    </div>
                    <div class="whf" id="yield_curve">
                        此处为数据曲线
                    </div>
                </div>
            </div>
            <div class="block_list">
                <p class="block_title fl">
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
                            <tr v-for="(transaction,index) in accountTransactionList" v-if="index>=(currentPage-1)*pageSize && index <= currentPage*pageSize -1">
                                <td>{{$global.myFormatTime(transaction.timestamp, 'YMDHMS')}}</td>
                                <td class="linker" @click="openBlockInfoDialog(transaction.height)">{{transaction.height}}</td>
                                <td v-if="transaction.type === 0">普通支付</td>
                                <td v-if="transaction.type === 1">任意信息</td>
                                <td v-if="transaction.type === 6">存储服务</td>
                                <td v-if="transaction.type === 9">出块奖励</td>
                                <td v-if="transaction.senderRS === accountInfo.accountRS && transaction.type !== 9">-{{$global.formatMoney(transaction.amountNQT/100000000)}} SS</td>
                                <td v-else>+{{$global.formatMoney(transaction.amountNQT/100000000)}} SS</td>
                                <td>{{$global.formatMoney(transaction.feeNQT/100000000)}} SS</td>
                                <td class=" image_text w300">
                                    <span class="linker" v-if="transaction.type === 9">Coinbase</span>
                                    <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)"
                                          v-else-if="transaction.senderRS === accountInfo.accountRS && transaction.type !== 9">您</span>
                                    <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)"
                                          v-else-if=" transaction.senderRS !== accountInfo.accountRS && transaction.type !== 9">{{transaction.senderRS}}</span>
                                    <img src="../../assets/right_arrow.svg"/>
                                    <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)" v-if="transaction.type === 9">您</span>
                                    <span class="linker" @click="openAccountInfoDialog(transaction.recipientRS)"
                                          v-else-if="transaction.recipientRS === accountInfo.accountRS && transaction.type !== 9">您</span>
                                    <span class="linker" @click="openAccountInfoDialog(transaction.recipientRS)"
                                          v-else-if="transaction.recipientRS !== accountInfo.accountRS && transaction.type !== 9">{{transaction.recipientRS}}</span>
                                </td>
                                <td>{{transaction.confirmations}}</td>
                                <td class="linker" @click="openTradingInfoDialog(transaction.transaction)">查看详情</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination" v-if="totalSize > pageSize">
                        <div class="list_pagination">
                            <el-pagination
                                @size-change="handleSizeChange"
                                @current-change="handleCurrentChange"
                                :current-page.sync="currentPage"
                                :page-size="pageSize"
                                layout="total, prev, pager, next, jumper"
                                :total="totalSize">
                            </el-pagination>
                        </div>
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
                            <el-form-item label="接收者" class="item_receiver">
                                <masked-input id="receiver" mask="AAA-****-****-****-*****" v-model="messageForm.receiver" />
                                <img src="../../assets/account_directory.svg"/>
                            </el-form-item>
                            <el-form-item label="秘钥" v-if="messageForm.hasPublicKey">
                                <el-input v-model="messageForm.publicKey" type="password"></el-input>
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
                                <el-slider v-model="messageForm.fee" show-input :show-tooltip="false" :max="100000">
                                </el-slider>
                            </el-form-item>
                            <el-form-item label="秘钥" v-if="typeof secretPhrase === 'undefined'">
                                <el-input v-model="messageForm.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="sendMessage">发送信息</button>
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
                            <el-form-item label="接收者" class="item_receiver">
                                <masked-input mask="AAA-****-****-****-*****" v-model="messageForm.receiver"/>
                                <img src="../../assets/account_directory.svg"/>
                            </el-form-item>
                            <el-form-item label="数额">
                                <el-input v-model="transfer.number"></el-input>
                                <label class="input_suffix">SS</label>
                            </el-form-item>
                            <el-form-item label="手续费">
                                <el-slider v-model="messageForm.fee" show-input :show-tooltip="false" :max="100000">
                                </el-slider>
                            </el-form-item>
                            <el-form-item label="">
                                <el-checkbox v-model="transfer.hasMessage">添加一条信息</el-checkbox>
                                <el-checkbox ref="encrypted2" v-model="transfer.isEncrypted"
                                             :disabled="ncryptedDisabled">加密信息
                                </el-checkbox>
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
                        <button type="button" class="btn" @click="">发送</button>
                    </div>
                </div>
            </div>
        </div>
        <!--view tranfer account dialog-->
        <div class="modal_hubSetting" id="hub_setting" v-show="hubSettingDialog">
            <div class="modal-header">
                <button class="common_btn" @click="openAdminDialog('reset')">重置Hub</button>
                <button class="common_btn" @click="openAdminDialog('restart')">重启Hub</button>
                <h4 class="modal-title">
                    <span>Hub设置</span>
                </h4>

            </div>
            <div class="modal-body">
                <div class="version_info">
                    <span>当前版本：</span>
                    <span>{{blockchainState.version}}</span>
                    <span v-if="isUpdate">发现新版本:{{latesetVersion}}</span>
                    <span v-if="isUpdate" @click="openAdminDialog('update')">点击更新</span>
                </div>
                <el-form label-position="left" label-width="160px">
                    <el-form-item label="启动内网穿透服务:">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item label="Sharder官网账户:">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item label="Sharder官网密码:" >
                        <el-input v-model="hubsetting.sharderPwd" @blur="checkSharder"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务地址:" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务端口:" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务客户端秘钥:"  v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="公网地址:" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.publicAddress" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="关联SS地址:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item label="是否开启挖矿:">
                        <el-checkbox v-model="hubsetting.isOpenMining">锻造已启用</el-checkbox>
                    </el-form-item>
                    <el-form-item label="改绑助记词:" v-if="hubsetting.isOpenMining">
                        <el-input v-model="hubsetting.modifyMnemonicWord"></el-input>
                    </el-form-item>
                    <el-form-item label="新密码:">
                        <el-input v-model="hubsetting.newPwd"></el-input>
                    </el-form-item>
                    <el-form-item label="确认新密码:">
                        <el-input v-model="hubsetting.confirmPwd"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn" @click="openAdminDialog('reConfig')">确认</button>
                    <button class="common_btn" @click="closeDialog()">取消</button>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="account_info" v-show="userInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>账户详情</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                    <tr>
                        <th>账户地址:</th>
                        <td>{{accountInfo.accountRS}}</td>
                    </tr>
                    <tr>
                        <th>账户名：</th>
                        <td>
                            <div class="accountName" v-if="isShowName">
                                <span v-if="accountInfo.name">{{accountInfo.name}}</span>
                                <span v-else style="color:#999;font-weight: normal">未设置</span>
                                <img src="../../assets/rewrite.svg" @click="isShowName = false"/>
                            </div>
                            <div class="rewriteName" v-else>
                                <el-input v-model="messageForm.receiver"></el-input>
                                <button class="common_btn" @click="isShowName = true">确认</button>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <th>账户余额：</th>
                        <td>{{$global.formatMoney(accountInfo.balanceNQT/100000000)}} SS</td>
                    </tr>
                    <tr>
                        <th>可用余额：</th>
                        <td>{{$global.formatMoney(accountInfo.effectiveBalanceSS)}} SS</td>
                    </tr>
                    <tr>
                        <th>挖矿余额：</th>
                        <td>{{$global.formatMoney(accountInfo.forgedBalanceNQT/100000000)}} SS</td>
                    </tr>
                    <tr>
                        <th>公钥</th>
                        <td>{{accountInfo.publicKey}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>

        </div>

        <dialogCommon :tradingInfoOpen="tradingInfoDialog" :trading="trading"
                      :accountInfoOpen="accountInfoDialog" :generatorRS="generatorRS"
                      :blockInfoOpen="blockInfoDialog" :height="height" @isClose="isClose"></dialogCommon>

        <adminPwd :openDialog="adminPasswordDialog" @getPwd="getAdminPassword" @isClose="isClose"></adminPwd>


    </div>
</template>
<script>
    import echarts from "echarts";
    import dialogCommon from "../dialog/dialog_common";
    import adminPwd from "../dialog/adminPwd";

    export default {
        name: "Network",
        components: {echarts,dialogCommon,adminPwd,
            "masked-input": require("vue-masked-input").default},
        data () {
            return {
                //dialog
                sendMessageDialog: false,
                tranferAccountsDialog: false,
                hubSettingDialog: false,

                tradingInfoDialog: false,
                userInfoDialog:false,
                accountInfoDialog: false,
                adminPasswordDialog:false,

                ncryptedDisabled: true,
                isShowName: true,

                generatorRS:'',
                secretPhrase:SSO.secretPhrase,

                blockInfoDialog:false,
                height:'',

                publicKey:SSO.publicKey,
                messageForm: {
                    errorCode:false,
                    receiver: "SSA",
                    message: "",
                    isEncrypted: false,
                    hasPublicKey:false,
                    publicKey:"",
                    file: [],
                    fee: 0,
                    password: ""
                },
                transfer: {
                    receiver: "",
                    number: 0,
                    fee: 0,
                    hasMessage: false,
                    message: "",
                    isEncrypted: false,
                    password: ""
                },
                hubsetting: {
                    openPunchthrough: true,
                    sharderAccount: '',
                    sharderPwd: '',
                    address:'',
                    port: '',
                    clientSecretkey: '',
                    publicAddress: '',
                    SS_Address: '',
                    isOpenMining: false,
                    modifyMnemonicWord: '',
                    newPwd: '',
                    confirmPwd: ''
                },
                blockchainState:this.$global.blockchainState,
                accountInfo:{
                    accountRS: SSO.accountRS,
                    publicKey: SSO.publicKey,
                    accountId: SSO.account,
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
                    label:'普通支付'
                },{
                    value:1,
                    label:'任意信息'
                },{
                    value:6,
                    label:'存储服务'
                },{
                    value:9,
                    label:'出块奖励'
                }],

                trading:'',
                accountTransactionList:[],
                //分页信息
                currentPage:1,
                totalSize:0,
                pageSize:10,

                latesetVersion:'',
                isUpdate:false,

                adminPasswordTitle:'',
                params:[],

            };
        },
        created(){
            console.log("----------------------------------",$('#receiver'));
            const _this = this;
            _this.getAccount(_this.accountInfo.accountRS);
            _this.getAccountTransactionList();
            _this.$global.setBlockchainState(_this).then(res=>{
                _this.blockchainState = res;
            });
            _this.$global.getUserConfig(_this).then(res=>{
                _this.hubsetting.address = res["sharder.NATServiceAddress"];
                _this.hubsetting.port = res["sharder.NATServicePort"];
                _this.hubsetting.clientSecretkey = res["sharder.NATClientKey"];
                _this.hubsetting.publicAddress = res["sharder.myAddress"];
                _this.hubsetting.SS_Address = res["sharder.HubBindAddress"];
            });
            _this.$http.get('/sharder?requestType=getLastestHubVersion').then(res=>{
                _this.latesetVersion = res.data.version;

                let bool = _this.versionCompare(_this.blockchainState.version, _this.latesetVersion);

                console.log("current version:",_this.blockchainState.version);
                console.log("lateset version:",_this.latesetVersion);
                console.log("bool:",bool);
                _this.isUpdate = bool;
            }).catch(err=>{
                console.log(err);
            });
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
                    tooltip: {
                        trigger: 'axis'
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
            drawYield: function () {
                const yieldCurve = echarts.init(document.getElementById("yield_curve"));

                const option = {
                    grid: {
                        left: '5%',
                        right: '2%',
                        top: '10%',
                        bottom: '15%',
                    },
                    tooltip: {
                        trigger: 'axis'
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
            handleSizeChange(val) {},
            handleCurrentChange(val) {},
            updateHubVersion(adminPwd){
                const _this = this;
                this.$http.post('/sharder?requestType=upgradeClient', {
                    version: _this.latesetVersion,
                    restart: true,
                    adminPassword: adminPwd
                }).then(res => {
                    if (res.data.upgraded) {
                        _this.$message.success('更新成功');
                    } else {
                        _this.$message.error(res.data.error);
                    }
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            restartHub(adminPwd){
                const _this = this;
                this.$http.post('/sharder?requestType=restart', {
                    adminPassword: adminPwd
                }).then(res => {
                    _this.$message.success('请稍后再次打开页面');
                }).catch(err => {
                    _this.$message.error(err);

                });
            },
            resettingHub(adminPwd){
                const _this = this;
                this.$http.post('/sharder?requestType=recovery', {
                    adminPassword: adminPwd,
                    restart: true
                }).then(res => {
                    _this.$message.success('请稍后再次打开页面');
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            updateHubSetting(adminPwd, params){
                const _this = this;
                params.append("adminPassword",adminPwd);
                this.$http.post('/sharder?requestType=reConfig', params).then(res => {
                    _this.$message.success('请稍后再次打开页面');
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            verifyHubSettingInfo(){
                const _this = this;
                let params = new URLSearchParams();

                if(_this.hubsetting.openPunchthrough){
                    params.append("sharder.useNATService",true);
                    if(_this.hubsetting.address === '' ||
                        _this.hubsetting.port === '' ||
                        _this.hubsetting.clientSecretkey === ''){
                        if(_this.hubsetting.sharderPwd === '')
                            _this.$message.error("请输入Sharder账号获取HUB配置信息");
                        else
                            _this.$message.error("请联系管理员获取Hub设置");
                        return false;
                    }else{
                        params.append("sharder.NATServiceAddress",_this.hubsetting.address);
                        params.append("sharder.NATServicePort",_this.hubsetting.port);
                        params.append("sharder.NATClientKey",_this.hubsetting.clientSecretkey);
                        params.append("sharder.myAddress", _this.hubsetting.publicAddress);
                    }
                }else{
                    params.append("sharder.useNATService",false);
                }

                if(_this.hubsetting.isOpenMining){
                    params.append("sharder.HubBind",true);
                    if(_this.hubsetting.SS_Address !== ''){
                        const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                        if(!_this.hubsetting.SS_Address.toUpperCase().match(pattern)){
                            _this.$message.warning('关联SS地址格式错误！');
                            return false;
                        }else{
                            params.append("sharder.HubBindAddress",_this.hubsetting.SS_Address);
                            params.append("reBind",true);
                        }
                    }
                    if(_this.hubsetting.modifyMnemonicWord !== '')
                        params.append("sharder.HubBindPassPhrase",_this.hubsetting.modifyMnemonicWord);
                }else{
                    params.append("sharder.HubBind",false);
                    params.append("reBind",false);
                }
                params.append("restart",false);

                if(_this.hubsetting.newPwd !== "" || _this.hubsetting.confirmPwd !== ""){
                    if(_this.hubsetting.newPwd !== _this.hubsetting.confirmPwd){
                        _this.$message.warning("密码不一致！");
                        return false;
                    }else{
                        params.append("newAdminPassword",_this.hubsetting.newPwd);
                    }
                }
                return params;
            },
            checkSharder(){
                const _this = this;
                if(_this.hubsetting.sharderAccount !== '' && _this.hubsetting.sharderPwd !== '' && _this.hubsetting.openPunchthrough){
                    _this.$http.post('https://taskhall.sharder.org/bounties/hubDirectory/check.ss',{
                        username:_this.hubsetting.sharderAccount,
                        password:_this.hubsetting.sharderPwd
                    }).then(res=>{
                        if(res.data.status === 'seccess'){
                            _this.hubsetting.address = res.data.data.natServiceAddress;
                            _this.hubsetting.port = res.data.data.natServicePort;
                            _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
                            _this.hubsetting.publicAddress = res.data.data.hubAddress;
                            _this.hubsetting.SS_Address = '';
                        }else if(res.data.errorType === 'unifiedUserIsNull'){
                            _this.$message.error(res.data.errorMessage);
                        }else if(res.data.errorType === 'hubDirectoryIsNull'){
                            _this.$message.error('暂无配置，请联系管理员');
                        }
                    })
                }
            },
            getAccount(account){
                const _this = this;
                return new Promise((resolve, reject) => {
                    this.$http.get('/sharder?requestType=getAccount', {
                        params: {
                            account: account,
                            includeLessors: true,
                            includeAssets: true,
                            includeEffectiveBalance: true,
                            includeCurrencies: true,

                        }
                    }).then(function (res) {
                        _this.accountInfo = res.data;
                        resolve(res.data);
                        console.log(_this.accountInfo);
                    }).catch(function (err) {
                        console.log(err);
                    });
                });
            },
            sendMessage:function(){
                const _this = this;
                console.log(_this.messageForm);
                if(_this.messageForm.receiver === ''){
                    _this.$message.warning('接收者不能为空');
                    return;
                }
                const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                if(!_this.messageForm.receiver.toUpperCase().match(pattern)){
                    _this.$message.warning('接收者ID格式错误！');
                    return;
                }
                if(_this.messageForm.hasPublicKey){
                    if(_this.messageForm.publicKey === ""){
                        _this.$message.warning('必须输入接收者公钥。');
                        return;
                    }
                }

                if(typeof _this.secretPhrase === 'undefined'){
                    if(_this.messageForm.password === ''){
                        _this.$message.warning('必须输入私钥。');
                        return;
                    }
                }else{
                    _this.messageForm.password = _this.secretPhrase;
                }

                if(!_this.messageForm.isEncrypted){
                    if(_this.messageForm.file === []){
                        _this.sendNormalMessage();
                    }else{

                    }
                }else{
                    if(_this.messageForm.file === []){

                    }else{

                    }
                }

                /*_this.http.get('/sharder?requestType=getAccont',{
                    params:{
                        account:_this.messageForm.receiver
                    }
                }).then(res =>{
                    _this.getAccount(_this.accountInfo.accountRS).then(res=>{
                        if(res.balanceNQT < _this.messageForm.fee){
                            _this.$message.warning('账户余额不足,请先充值后再试！');
                        }else{
                            if(!res.data.errorDescription){
                                if(_this.messageForm.isEncrypted){
                                    _this.$http.post('/sharder?requestType=sendMessage',{
                                        recipient:_this.messageForm.receiver,
                                        recipientPublicKey:res.data.publicKey,
                                        messageToEncrypt :_this.messageForm.message,
                                        feeNQT:_this.messageForm * 100000000,

                                    }).then(res =>{

                                    }).catch(err =>{

                                    });
                                }
                            }
                        }
                    });
                })*/
            },
            sendNormalMessage:function(){
                const _this = this;
                let params = new URLSearchParams();

                params.append("recipient",_this.messageForm.receiver);
                params.append("recipientPublickkey",_this.messageForm.publicKey);
                params.append("meesage",_this.messageForm.message);
                params.append("secretPhrase",_this.messageForm.password);
                params.append("deadline",1440);
                params.append("phased",false);
                params.append("phasingHashedSecretAlgorithm",2);
                params.append("messagelsText",false);
                params.append("feeNQT",_this.messageForm.fee*100000000)

                _this.$http.post('/sharder?requestType=sendMessage',params).then(res=>{
                    if(typeof res.data.errorDescription !== 'undefined'){
                        _this.$message.error(res.data.errorDescription);
                    }else{
                        _this.$message.success("发送成功！");
                        _this.closeDialog();
                    }
                }).catch(err=>{
                    _this.$message.error(err);
                });
            },
            getAccountTransactionList:function(){
                const _this = this;
                // console.log("第"+i+"次");
                this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                    params:{
                        account:_this.accountInfo.accountRS,
                        type:_this.selectType,
                    }
                }).then(function (res) {
                    _this.accountTransactionList =res.data.transactions;
                    console.log(_this.accountTransactionList);
                    _this.totalSize = _this.accountTransactionList.length;
                    // _this.newCount = res.data.transactions.length;

                }).catch(function (err) {
                    console.log(err);
                });
            },
            openSendMessageDialog: function () {
                this.$store.state.mask = true;
                this.sendMessageDialog = true;
            },
            openTransferDialog: function () {
                this.$store.state.mask = true;
                this.tranferAccountsDialog = true;
            },
            openHubSettingDialog: function () {
                const _this = this;
                _this.$store.state.mask = true;
                _this.hubSettingDialog = true;
                console.log(_this.hubsetting);

            },
            openTradingInfoDialog:function(trading){
                this.trading = trading;
                this.tradingInfoDialog = true;
                // this.$store.state.mask = true;
                // this.tradingInfoDialog = true;
            },
            openUserInfoDialog:function(){
                this.userInfoDialog = true;
                this.$store.state.mask = true;
            },
            openAccountInfoDialog: function (account) {
                // this.$store.state.mask = true;
                // this.accountInfoDialog = true;
                this.generatorRS = account;
                console.log(account);
                this.accountInfoDialog = true;
            },
            openBlockInfoDialog:function(height){
                this.height = height;
                this.blockInfoDialog = true;
            },
            openAdminDialog:function(title){
                const _this = this;
                _this.adminPasswordTitle = title;

                if(title === 'reConfig'){
                    let info = _this.verifyHubSettingInfo();
                    if(info === false){
                        return;
                    }else{
                        _this.params = info;
                    }
                }
                _this.hubSettingDialog = false;
                _this.adminPasswordDialog =true;
            },
            getAdminPassword:function(adminPwd){
                const _this = this;
                _this.adminPassword = adminPwd;
                _this.adminPasswordDialog =false;
                if(_this.adminPasswordTitle === 'reset'){
                    _this.resettingHub(adminPwd);
                }else if(_this.adminPasswordTitle === 'restart'){
                    _this.restartHub(adminPwd);
                }else if(_this.adminPasswordTitle === 'update'){
                    _this.updateHubVersion(adminPwd);
                }else if(_this.adminPasswordTitle === 'reConfig'){
                    _this.updateHubSetting(adminPwd,_this.params);
                }
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.sendMessageDialog = false;
                this.tranferAccountsDialog = false;
                this.hubSettingDialog = false;
                this.tradingInfoDialog = false;
                this.accountInfoDialog = false;
                this.userInfoDialog = false;
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
            },
            isClose:function () {
                const _this = this;
                _this.tradingInfoDialog = false;
                _this.accountInfoDialog = false;
                _this.blockInfoDialog = false;
                _this.adminPasswordDialog = false;
            },
            versionCompare(current, latest){
                let currentPre = parseFloat(current);
                let latestPre = parseFloat(latest);
                let currentNext =  current.replace(currentPre + ".","");
                let latestpreNext =  latest.replace(latestPre + ".","");
                if(currentPre > latestPre){
                    return false;
                }else if(currentPre < latestPre){
                    return true;
                }else{
                    if(currentNext >= latestpreNext){
                        return false;
                    }else{
                        return true;
                    }
                }
            }
        },
        watch: {
            transfer: {
                handler(val, oldVal) {
                    const _this = this;
                    if (_this.transfer.hasMessage) {
                        _this.ncryptedDisabled = false;
                    } else {
                        _this.ncryptedDisabled = true;
                        _this.transfer.isEncrypted = false;
                    }
                },
                deep: true
            },
            hubsetting: {
                handler(val, oldVal) {
                    const _this = this;
                    if (_this.hubsetting.openPunchthrough) {
                        _this.checkSharder();
                    }

                },
                deep: true
            },
            messageForm:{
                handler(val,oldVal){
                    const _this = this;
                    if(_this.messageForm.receiver !== ''){
                        console.log("receiver",_this.messageForm.receiver);
                    }
                },
                deep:true
            },
            selectType:function () {
                const _this = this;
                _this.getAccountTransactionList();
            }
        },
        mounted() {
            const _this = this;
            this.drawBarchart();
            this.drawYield();
            $('#receiver').on("blur",function() {
                let receiver = _this.messageForm.receiver;
                console.log(receiver);
                if(receiver !== "___-____-____-____-_____" && receiver !== "SSA-____-____-____-_____"){
                    const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                    if(!receiver.toUpperCase().match(pattern)){
                        _this.$message.warning('接收者ID格式错误！');
                        return;
                    }
                    if(receiver === _this.accountInfo.accountRS){
                        _this.$message.warning("这是您的账户");
                        _this.messageForm.errorCode = true;
                    }
                    _this.getAccount(receiver).then(res=>{
                        console.log(res);
                        if(res.errorDescription === "Unknown account") {
                            _this.messageForm.hasPublicKey = true;
                            _this.messageForm.errorCode = true;
                            _this.$message.warning("接收者帐户是未知帐户，意味着它没有转入或转出的交易记录。您可以通过提供接收者的公钥来增加安全性。");
                        }else if(res.errorDescription === "Incorrect \"account\""){
                            _this.messageForm.errorCode = true;
                            _this.messageForm.hasPublicKey = false;
                            _this.$message.warning("接收者的帐户格式不正确，请调整。");
                        }else if(typeof res.errorDescription === "undefined"){
                            _this.messageForm.errorCode = false;
                            _this.messageForm.hasPublicKey = false;
                            _this.messageForm.publicKey = res.publicKey;

                        }
                    });
                }
            });
        },
    };


</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped lang="scss" type="text/scss">
    .el-select-dropdown {
        .el-select-dropdown__item.selected {
            background-color: #493eda !important;
            color: #fff !important;
        }
        .el-select-dropdown__item.selected.hover {
            background-color: #493eda !important;
            color: #fff !important;
        }
    }
    .item_receiver{
        input{
            padding-left: 15px;
        }
        img{
            width: 20px;
            position: absolute;
            right: 15px;
            top: 40px;
        }
    }
</style>
