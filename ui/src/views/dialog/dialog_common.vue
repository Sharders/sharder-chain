<template>
    <div>
        <!--view account info-->
        <div class="modal_info" id="account_info" v-show="accountInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/error.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{accountInfo.accountRS}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span v-if="accountInfo.name">{{$t('dialog.account_info_name') + ': ' + accountInfo.name}} | </span>
                    <span>{{$t('dialog.account_info_available_asset') + ': ' + $global.getSSNumberFormat(accountInfo.unconfirmedBalanceNQT)}}</span>
                </div>
                <div class="account_allInfo">
                    <el-radio-group v-model="tabTitle" class="title">
                        <el-radio-button label="account" class="btn">{{$t('dialog.account_info_total_transaction')}}
                        </el-radio-button>
                    </el-radio-group>

                    <div v-if="tabTitle === 'account'" class="account_list">
                        <table class="table">
                            <tr>
                                <th>{{$t('dialog.account_transaction_time')}}</th>
                                <th>{{$t('dialog.account_transaction_id')}}</th>
                                <th>{{$t('dialog.account_transaction_type')}}</th>
                                <th>{{$t('dialog.account_transaction_amount')}}</th>
                                <th>{{$t('dialog.account_info_fee')}}</th>
                                <th>{{$t('dialog.account_info_account')}}</th>
                                <th>{{$t('dialog.account_info_operating')}}</th>
                                <th class="gutter"></th>
                            </tr>
                        </table>
                        <div class="table_body">
                            <table class="table">
                                <tbody>
                                <tr v-for="transactions in accountTransactionInfo">
                                    <td>
                                        <span>{{$global.myFormatTime(transactions.timestamp,'YMDHMS',true)}}</span><br>
                                        <span class="utc-time">{{$global.formatTime(transactions.timestamp)}} +UTC</span>
                                    </td>
                                    <td>{{transactions.transaction}}</td>
                                    <td class="transaction-img">
                                        <span class="bg"
                                              :class="'type' + transactions.type + transactions.subtype"></span>
                                        <span>{{$global.getTransactionTypeStr(transactions)}}</span>
                                    </td>
                                    <td>{{$global.getTransactionAmountNQT(transactions,accountInfo.accountRS)}}</td>
                                    <td>{{$global.getTransactionFeeNQT(transactions)}}</td>
                                    <td class="linker" style="font-size:11px;" @click="checkAccountInfo(transactions.senderRS)">
                                        <span>{{transactions.senderRS}}</span>
                                    </td>
                                    <td class="linker" style="font-size:11px;" @click="openTransactionDialog(transactions.transaction)">
                                        {{$t('dialog.account_info_view_detail')}}
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--view account transaction info-->
        <div class="modal_info" id="account_transaction" v-show="accountTransactionDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/error.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.account_info_title1')}} - {{accountInfo.accountRS}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span v-if="accountInfo.name">{{$t('dialog.account_info_name') + accountInfo.name}} |</span>
                    <span>{{$t('dialog.account_info_available_asset') + ": " + $global.getSSNumberFormat(accountInfo.unconfirmedBalanceNQT)}}</span>
                </div>
                <div class="account_transactionInfo">
                    <p class="fl">{{$t('dialog.account_transaction_detail')}}</p>
                    <button class="fr writeBtn" @click="openAccountInfo(accountInfo.accountRS)">
                        {{$t('dialog.account_transaction_return')}}
                    </button>
                    <div class="cb"></div>
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>{{$t('dialog.account_transaction_block_height')}}</th>
                            <td>{{$global.returnObj(transactionInfo.block,transactionInfo.height)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_transaction_serial_number')}}</th>
                            <td>{{$global.returnObj(transactionInfo.transactionIndex,transactionInfo.transactionIndex)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_id')}}</th>
                            <td>{{transactionInfo.transaction}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_type')}}</th>
                            <td>{{$global.getTransactionTypeStr(transactionInfo)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_sender')}}</th>
                            <td>{{$global.getSenderRSOrWo(transactionInfo)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_sender_public_key')}}</th>
                            <td>{{transactionInfo.senderPublicKey}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_signature')}}</th>
                            <td>{{transactionInfo.signature}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_signatureHash')}}</th>
                            <td>{{transactionInfo.signatureHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_amount')}}</th>
                            <td>{{$global.getTransactionAmountNQT(transactionInfo,accountInfo.accountRS)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_info_fee')}}</th>
                            <td>{{$global.getTransactionFeeNQT(transactionInfo)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                            <td>{{$global.getSenderOrRecipient(transactionInfo)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('transaction.transaction_confirm_quantity')}}</th>
                            <td>{{$global.returnObj(transactionInfo.block,transactionInfo.confirmations)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_version')}}</th>
                            <td>{{transactionInfo.version}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_fullHash')}}</th>
                            <td>{{transactionInfo.fullHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_block_timestamp')}}</th>
                            <td>{{$global.getTransactionBlockTimestamp(transactionInfo)}}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
        <!--view block info-->
        <div class="modal_info" id="block_info" v-show="blockInfoDialog">

            <div class="modal-header">
                <img class="close" src="../../assets/img/error.svg" @click="closeDialog()"/>
                <h4 class="modal-title">
                    <span>
                        {{$t('dialog.block_info_title1')}} - {{blockInfo.block}} | {{$t('dialog.account_transaction_block_height')}} - {{blockInfo.height}}
                    </span>
                </h4>
            </div>
            <div class="modal-body">
                <el-radio-group v-model="tabTitle" class="title">
                    <el-radio-button label="account" class="btn">{{$t('dialog.block_info_all_transaction')}}
                    </el-radio-button>
                    <el-radio-button label="blockInfo" class="btn">{{$t('dialog.block_info_all_block_detail')}}
                    </el-radio-button>
                    <el-radio-button v-if="pocInfoList.length > 0" label="pocInfo" class="btn">PoC</el-radio-button>
                    <el-radio-button v-if="poolInfoList.length > 0" label="poolInfo" class="btn">Pool</el-radio-button>
                </el-radio-group>

                <div v-if="tabTitle === 'account'" class="account_list">
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>{{$t('dialog.account_transaction_time')}}</th>
                            <th>{{$t('dialog.account_transaction_id')}}</th>
                            <th>{{$t('dialog.account_transaction_type')}}</th>
                            <th>{{$t('dialog.account_transaction_amount')}}</th>
                            <th>{{$t('dialog.account_info_fee')}}</th>
                            <th>{{$t('dialog.account_transaction_sender')}}</th>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                        </tr>
                        <tr v-for="(transaction,index) in blockInfo.transactions">
                            <td>
                                <span>{{$global.myFormatTime(transaction.timestamp,'YMDHMS',true)}}</span><br>
                                <span class="utc-time">{{$global.formatTime(transaction.timestamp)}} +UTC</span>
                            </td>
                            <td class="linker" @click="openTransactionDialog(transaction.transaction)">{{transaction.transaction}}</td>
                            <td class="transaction-img">
                                <span class="bg" :class="'type' + transaction.type + transaction.subtype"></span>
                                <span>{{$global.getTransactionTypeStr(transaction)}}</span>
                            </td>
                            <td>{{$global.getTransactionAmountNQT(transaction,"")}}</td>
                            <td>{{$global.getTransactionFeeNQT(transaction)}}</td>
                            <td v-if="transaction.type === 9">CoinBase</td>
                            <td class="linker" style="font-size:11px;" v-else @click="openAccountInfo(transaction.senderRS)">
                                {{transaction.senderRS}}
                            </td>
                            <td class="linker" style="font-size:11px;" v-if="transaction.type === 9"
                                @click="openAccountInfo(transaction.senderRS)">
                                {{transaction.senderRS}}
                            </td>
                            <td class="linker" style="font-size:11px;" v-else @click="openAccountInfo(transaction.recipientRS)">
                                {{transaction.recipientRS}}
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div v-if="tabTitle === 'blockInfo'" class="blockInfo">
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>{{$t('dialog.account_transaction_block_height')}}</th>
                            <td>{{blockInfo.height}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_previous_block_hash')}}</th>
                            <td>{{blockInfo.previousBlockHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_cumulative_difficulty')}}</th>
                            <td>{{blockInfo.cumulativeDifficulty}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_transcation_amount')}}</th>
                            <td>{{blockInfo.numberOfTransactions}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_payload_length')}}</th>
                            <td>{{blockInfo.payloadLength}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_total_amount')}}</th>
                            <td>{{$global.getBlocKTotalAmountNQT(blockInfo.totalAmountNQT)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_total_fee')}}</th>
                            <td>{{$global.getBlockTotalFeeNQT(blockInfo.totalFeeNQT)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_mining')}}</th>
                            <td class="linker" @click="openAccountInfo(blockInfo.generatorRS)">
                                {{blockInfo.generatorRS}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_generation_public_key')}}</th>
                            <td>{{blockInfo.generatorPublicKey}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_generation_signature')}}</th>
                            <td>{{blockInfo.generationSignature}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_block_signature')}}</th>
                            <td>{{blockInfo.blockSignature}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_version')}}</th>
                            <td>{{blockInfo.version}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_timestamp')}}</th>
                            <td>{{$global.getFormattedTimestamp(blockInfo.totalFeeNQT)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_previous_block')}}</th>
                            <td class="linker" @click="openBlockInfo(blockInfo.previousBlock)">
                                {{blockInfo.previousBlock}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_next_block')}}</th>
                            <td class="linker" v-if="blockInfo.nextBlock" @click="openBlockInfo(blockInfo.nextBlock)">
                                {{blockInfo.nextBlock}}
                            </td>
                            <td v-else></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div v-if="tabTitle === 'pocInfo'" class="blockInfo">
                    <el-table :data="pocInfoList" class="poc" style="width: 100%">
                        <el-table-column type="expand">
                            <template slot-scope="props">
                                <el-form label-position="left" inline>
                                    <el-row>
                                        <PocDetailContent :rowData="props.row"></PocDetailContent>
                                    </el-row>
                                </el-form>
                            </template>
                        </el-table-column>
                        <el-table-column
                            align="center"
                            :label="$t('poc.linkedAccount')"
                            :formatter="rsAccount"
                            >
                            <!--<template>
                                <div :id="'EID_'+{{accountId}}"></div>
                            </template>-->
                        </el-table-column>
                        <el-table-column
                            prop="subType"
                            align="center"
                            :label="$t('poc.type')"
                            :formatter="parseSubType">
                        </el-table-column>
                        <el-table-column
                            prop="ip"
                            align="center"
                            :label="$t('poc.ip')">
                        </el-table-column>
                        <el-table-column
                            prop="nodeType"
                            align="center"
                            :label="$t('poc.nodeType')"
                            :formatter="parseNodeType">
                        </el-table-column>

                        <el-table-column
                            prop="heightandblock"
                            align="center"
                            :label="$t('poc.heightandblock_id')">
                        </el-table-column>
                        <el-table-column
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                    </el-table>
                </div>
                <div v-if="tabTitle === 'poolInfo'" class="blockInfo">
                    <el-table :data="poolInfoList" class="pool" style="width: 100%">
                        <el-table-column type="expand">
                            <template slot-scope="props">
                                <el-form label-position="left" inline>
                                    <el-row>
                                        <PoolTxDetail :rowData="props.row"></PoolTxDetail>
                                    </el-row>
                                </el-form>
                            </template>
                        </el-table-column>
                        <el-table-column
                            prop="senderRS"
                            align="center"
                            :label="$t('poc.creator')">
                        </el-table-column>
                        <el-table-column
                            prop="subType"
                            align="center"
                            :label="$t('poc.type')"
                            :formatter="parseSubTypePool">
                        </el-table-column>
                        <el-table-column
                            prop="height"
                            align="center"
                            :label="$t('poc.started_height')">
                        </el-table-column>
                        <el-table-column
                            prop="block"
                            align="center"
                            :label="$t('poc.started_block')">
                        </el-table-column>

                        <el-table-column
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                    </el-table>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="trading_info" v-show="tradingInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/error.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.account_transaction_detail')}} - {{transactionInfo.transaction}}</span>
                </h4>
            </div>
            <div class="modal-body">

                <table class="table">
                    <tbody>
                    <tr>
                        <th>{{$t('dialog.account_transaction_block_height')}}</th>
                        <td>{{$global.returnObj(transactionInfo.block,transactionInfo.height)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_transaction_serial_number')}}</th>
                        <td>{{$global.returnObj(transactionInfo.transactionIndex,transactionInfo.transactionIndex)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_type')}}</th>
                        <td>{{$global.getTransactionTypeStr(transactionInfo)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_sender')}}</th>
                        <td>{{$global.getSenderRSOrWo(transactionInfo)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_sender_public_key')}}</th>
                        <td>{{transactionInfo.senderPublicKey}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_signature')}}</th>
                        <td>{{transactionInfo.signature}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_signatureHash')}}</th>
                        <td>{{transactionInfo.signatureHash}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_amount')}}</th>
                        <td>{{$global.getTransactionAmountNQT(transactionInfo,accountInfo.accountRS)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_info_fee')}}</th>
                        <td>{{$global.getTransactionFeeNQT(transactionInfo)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_recipient')}}</th>
                        <td>{{$global.getSenderOrRecipient(transactionInfo)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('transaction.transaction_confirm_quantity')}}</th>
                        <td>{{$global.returnObj(transactionInfo.block,transactionInfo.confirmations)}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_version')}}</th>
                        <td>{{transactionInfo.version}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_fullHash')}}</th>
                        <td>{{transactionInfo.fullHash}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_block_timestamp')}}</th>
                        <td>{{$global.getTransactionBlockTimestamp(transactionInfo)}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>

        </div>
    </div>
</template>

<script>
    import PoolTxDetail from "./poolTxDetail";
    export default {
        name: "dialog_all",
        components: {PoolTxDetail},
        props: {
            accountInfoOpen: Boolean,
            blockInfoOpen: Boolean,
            tradingInfoOpen: Boolean,
            isSearch: Boolean,
            searchValue: '',
            generatorRS: '',
            trading: '',
            height: '',

        },
        data() {
            return {
                tabTitle: 'account',
                accountInfoDialog: this.accountInfoOpen,
                accountInfo: [],
                accountTransactionInfo: [],
                accountTransactionDialog: false,
                transactionInfo: [],
                transaction: '',
                accountRS: this.generatorRS,
                searchVal: '',
                blockInfoDialog: this.blockInfoOpen,
                blockInfo: [],
                pocInfoList:[],
                poolInfoList:[],
                accountIdMap:[],
                tradingInfoDialog: this.tradingInfoOpen,
                rs:'',
            }
        },
        methods: {

            httpGetAccountInfo(accountID) {
                const _this = this;
                return new Promise((resolve, reject) => {

                    _this.$http.get('/sharder?requestType=getAccount', {
                        params: {
                            account: accountID
                        }
                    }).then(function (res) {
                        if (!res.data.errorDescription) {
                            _this.accountInfo = res.data;
                            _this.$http.get('/sharder?requestType=getBlockchainTransactions', {
                                params: {
                                    account: accountID
                                }
                            }).then(function (res) {
                                _this.accountTransactionInfo = res.data.transactions;
                            }).catch(function (err) {
                                resolve(err);
                            });
                            resolve("success");
                        } else {
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                })
            },

            httpGetBlockInfo(height, BlockID) {
                const _this = this;
                _this.pocInfoList = [];
                _this.poolInfoList = [];
                return new Promise((resolve, reject) => {
                    _this.$http.get('/sharder?requestType=getBlock', {
                        params: {
                            height: height,
                            block: BlockID,
                            includeTransactions: true,
                        }
                    }).then(function (res) {
                        if (!res.data.errorDescription) {
                            _this.blockInfo = res.data;
                            let accoutIdArray = [];
                            for (let t of res.data.transactions) {
                                if (t.type === 12) {
                                    _this.pocInfoList.push({
                                        pocInfo:t.attachment,
                                        senderRS:t.senderRS,
                                        block:t.block,
                                        height:t.height,
                                        heightandblock:t.height+"/"+t.block,
                                        transaction:t.transaction,
                                        subType:t.subtype,
                                        ip:t.attachment.ip,
                                        nodeType:t.attachment.type,
                                        accountId:t.attachment.accountId

                                    });
                                    accoutIdArray.push(t.attachment.accountId);
                                }else if (t.type === 8) {
                                    _this.poolInfoList.push({
                                        poolInfo:t.attachment,
                                        senderRS:t.senderRS,
                                        block:t.block,
                                        height:t.height,
                                        transaction:t.transaction,
                                        subType:t.subtype,
                                        feeNQT:t.feeNQT,
                                    });

                                }

                            }
                            _this.$http.get('/sharder?requestType=getAccountId', {
                                params: {
                                    accoutId: accoutIdArray.join(","),
                                }
                            }).then(function (res) {
                                if (!res.data.errorDescription) {
                                    console.info(res.data);
                                    console.info("rs:"+_this.rs);
                                    _this.accountIdMap = res.data.rsAccountInfo;
                                    console.info("map:"+_this.accountIdMap);


                                } else {
                                    console.info(res.data.errorDescription);
                                }
                            }).catch(function (err) {
                                console.info(err);
                            });

                            resolve("success");
                        } else {
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                });
            },
            httpGetTradingInfo(tradingID) {
                const _this = this;
                return new Promise((resolve, reject) => {
                    this.$http.get('/sharder?requestType=getTransaction', {
                        params: {
                            transaction: tradingID
                        }
                    }).then(function (res) {
                        if (!res.data.errorDescription) {
                            _this.transactionInfo = res.data;
                            resolve("success");
                        } else {
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                })
            },
            checkAccountInfo(account) {
                const _this = this;
                _this.httpGetAccountInfo(account).then(res => {
                    if (res !== "success") {
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    }
                });
            },
            openTransactionDialog(transaction) {
                const _this = this;
                _this.httpGetTradingInfo(transaction).then(res => {
                    if (res !== "success") {
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    } else {
                        _this.$store.state.mask = true;
                        _this.accountInfoDialog = false;
                        _this.blockInfoDialog = false;
                        _this.accountTransactionDialog = true;
                    }
                });
            },
            openAccountInfo: function (accountRS) {
                const _this = this;
                if (accountRS) {
                    _this.httpGetAccountInfo(accountRS).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.accountTransactionDialog = false;
                            _this.blockInfoDialog = false;
                            _this.accountInfoDialog = true;
                        }
                    });
                } else {
                    _this.httpGetAccountInfo(_this.accountRS).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.accountTransactionDialog = false;
                            _this.blockInfoDialog = false;
                            _this.accountInfoDialog = true;
                        }
                    });
                }
            },
            openBlockInfo: function (blockId) {
                const _this = this;
                _this.httpGetBlockInfo('', blockId).then(res => {
                    if (res !== "success") {
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    } else {
                        _this.$store.state.mask = true;
                        _this.accountTransactionDialog = false;
                        _this.accountInfoDialog = false;
                        _this.tabTitle = "account";
                    }
                });
            },
            closeDialog: function () {
                const _this = this;
                _this.$store.state.mask = false;
                _this.accountInfoDialog = false;
                _this.accountTransactionDialog = false;
                _this.blockInfoDialog = false;
                _this.tradingInfoDialog = false;

                _this.accountTransactionInfo = [];
                _this.accountInfo = [];
                _this.transactionInfo = [];
                _this.blockInfo = [];
                _this.tabTitle = "account";
                _this.$emit('isClose', false);
            },
            parseSubType(row, column) {
                let subtype = row.subType;
                /*console.log("poc")*/
                switch (subtype) {
                    case 0:
                        return this.$root.$t("transaction.transaction_type_poc_node_type");
                    case 1:
                        return this.$root.$t("transaction.transaction_type_poc_node_config");
                    case 2:
                        return this.$root.$t("transaction.transaction_type_poc_weight_table");
                    case 3:
                        return this.$root.$t("transaction.transaction_type_poc_online");
                    case 4:
                        return this.$root.$t("transaction.transaction_type_poc_block_missing");
                    case 5:
                        return this.$root.$t("transaction.transaction_type_poc_bc_speed");
                    default:
                        return this.$root.$t("transaction.transaction_type_poc");
                }
            },
            parseNodeType(row,column) {
                let type = row.pocInfo.type;
/*                console.info("type:"+type);*/
                switch (type) {
                    case 1:
                        return this.$root.$t("poc.sharder_node");
                    case 2:
                        return this.$root.$t("poc.community_node");
                    case 3:
                        return this.$root.$t("poc.normal_node");
                    case 4:
                        return this.$root.$t("poc.hub_node");
                    case 5:
                        return this.$root.$t("poc.box_node");
                    default:
                        return this.$root.$t("poc.normal_node");
                }
            },
            parseSubTypePool(row, column) {
                let subtype = row.subType;
                console.log("pool")
                switch (subtype) {
                    case 0:
                        return this.$root.$t("transaction.transaction_type_pool_create");
                    case 1:
                        return this.$root.$t("transaction.transaction_type_pool_destroy");
                    case 2:
                        return this.$root.$t("transaction.transaction_type_pool_join");
                    case 3:
                        return this.$root.$t("transaction.transaction_type_pool_quit");
                    default:
                        return this.$root.$t("transaction.transaction_type_forge_pool");
                }
            },
            rsAccount:function(row,column) {
                const _this = this;
                let accountId = row.pocInfo.accountId;
                let accountIdMaps = _this.accountIdMap;
                if(!accountId){
                    return "--";
                }else{
                    for(let t of accountIdMaps){

                        if(t.accountId == accountId){
                            return t.rsaccountId;
                        }
                    }

                }

            },

        },
        filter:{

        },
        watch: {

            blockInfoOpen: function (val) {
                const _this = this;
                if (val) {
                    _this.httpGetBlockInfo(_this.height, '').then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.blockInfoDialog = true;
                            _this.accountInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                        }
                    });
                }
            },
            accountInfoOpen: function (val) {
                const _this = this;
                if (val) {
                    _this.httpGetAccountInfo(_this.generatorRS).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.accountInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                            _this.accountRS = _this.generatorRS;
                        }
                    });
                }
            },

            tradingInfoOpen: function (val) {
                const _this = this;
                if (val) {
                    _this.httpGetTradingInfo(_this.trading).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.tradingInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.accountInfoDialog = false;
                        }
                    });
                }
            },
            isSearch: function (val) {
                const _this = this;
                _this.searchVal = _this.searchValue;
                if (val) {
                    _this.httpGetAccountInfo(_this.searchVal).then(function (res) {
                        if (res === 'success') {
                            _this.$store.state.mask = true;
                            _this.accountInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                            _this.accountRS = _this.searchVal;
                        } else {
                            _this.httpGetBlockInfo('', _this.searchVal).then(function (res) {
                                if (res === 'success') {
                                    _this.$store.state.mask = true;
                                    _this.blockInfoDialog = true;
                                    _this.accountInfoDialog = false;
                                    _this.accountTransactionDialog = false;
                                    _this.tradingInfoDialog = false;
                                } else {
                                    _this.httpGetTradingInfo(_this.searchVal).then(function (res) {
                                        if (res === 'success') {
                                            _this.$store.state.mask = true;
                                            _this.tradingInfoDialog = true;
                                            _this.blockInfoDialog = false;
                                            _this.accountTransactionDialog = false;
                                            _this.accountInfoDialog = false;
                                        } else {
                                            _this.$message({
                                                showClose: true,
                                                message: _this.$t('notification.search_null_info_error'),
                                                type: "error"
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    }).catch(err => {
                        console.log(err);
                    });
                    _this.$emit('isClose', false);
                }
            }
        }
    }
</script>

<style scoped type="text/scss" lang="scss">
    #block_info {
        .modal-body {
            max-height: 600px;
            overflow-y: auto;
            margin: 30px 20px 40px !important;

            .title {
                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: #493eda;
                }

                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }

                .el-radio-button__inner:hover {
                    color: #493eda;
                }
            }

            .account_list {
                .table {
                    border: none !important;
                    word-break: break-all;
                    margin-top: 33px;

                    td {
                        border: none !important;
                        text-align: center;
                        line-height: 20px;
                        font-size: 12px;
                        padding: 0;

                        img {
                            width: 12px;
                            vertical-align: middle;
                        }

                        span {
                            vertical-align: middle;
                        }
                    }

                    th {
                        border: none !important;
                    }

                    .linker {
                        color: #493eda;
                        cursor: pointer;

                        a {
                            margin: 0 6px;
                        }
                    }
                }
            }

            .blockInfo {
                .table {
                    word-break: break-all;
                    margin-top: 20px;

                    th {
                        width: 180px;
                    }

                    td {
                        width: 1000px;
                        padding: 8px 10px;
                        line-height: 20px;
                    }
                }
                
                .poc {
                    margin-top: 20px;
                }
            }
        }
    }

    #account_info {
        .modal-header {
            padding: 0 20px 0 40px;
        }

        .modal-body {
            margin: 0;

            .account_preInfo {
                background: #f4f7fd;
                width: 100%;
                padding-left: 41px;
                height: 60px;

                span {
                    line-height: 60px;
                    font-size: 16px;
                    color: #333;
                }
            }

            .account_allInfo {
                margin: 20px;

                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: #493eda;
                }

                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }

                .el-radio-button__inner:hover {
                    color: #493eda;
                }

                .account_list {
                    max-height: 510px;

                    .table {
                        border: none !important;
                        word-break: break-all;
                        margin-top: 33px;

                        tr {
                            height: 40px;

                            &:nth-child(even) {
                                background: #f4f7fc;
                            }
                        }

                        td {
                            border: none !important;
                            width: 190px;
                            text-align: center;
                            /*line-height: 40px;*/
                            font-size: 12px;
                            padding: 0;

                            &:nth-child(5) {
                                width: 205px;
                            }

                            img {
                                width: 12px;
                                vertical-align: middle;
                            }

                            span {
                                vertical-align: middle;
                            }
                        }

                        th {
                            border: none !important;
                            width: 190px;
                            padding: 0;
                            text-align: center;
                            font-weight: bold;

                            &:nth-child(5) {
                                width: 205px;
                            }

                        }

                        .linker {
                            color: #493eda;
                            cursor: pointer;

                            a {
                                margin: 0 6px;
                            }
                        }

                        .gutter {
                            width: 16px;
                            padding: 0;
                        }
                    }

                    .table_body {
                        overflow: auto;
                        max-height: 450px;

                        .table {
                            margin-top: 0;
                        }
                    }

                }
            }
        }
    }

    #account_transaction {
        /*top: 20px;*/

        .modal-header {
            padding: 0 40px;
            border-bottom: none;
        }

        .modal-body {
            margin: 0;

            .account_preInfo {
                background: #f4f7fd;
                width: 100%;
                padding-left: 41px;
                height: 60px;

                span {
                    line-height: 60px;
                    font-size: 16px;
                    color: #333;
                }
            }

            .account_transactionInfo {
                margin: 0 30px 20px;

                p {
                    line-height: 80px;
                    font-size: 17px;
                    font-weight: bold;
                    margin-left: 11px;
                    color: #000;
                }

                button {
                    margin: 20px 0;
                    padding: 0 20px;
                    height: 40px;
                }

                .table {
                    word-break: break-all;
                    //margin-top: 20px;
                    th {
                        width: 160px;
                    }

                    td {
                        width: 1000px;
                        padding: 6px 10px;
                        line-height: 20px;
                    }
                }
            }
        }
    }

    #trading_info {
        top: 80px;

        .table {
            td {
                width: 1000px;
                padding: 6px 10px;
                line-height: 20px;
            }

            th {
                width: 160px;
            }
        }
    }
</style>
