<template>
    <div>
        <!--view account info-->
        <div class="modal_info" id="account_info" v-show="accountInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.account_info_title1')}}{{accountInfo.accountRS}}{{$t('dialog.account_info_title2')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span v-if="typeof accountInfo.name !== 'undefined' && accountInfo.name !== ''">{{$t('dialog.account_info_name')}}&nbsp</span>
                    <span
                        v-if="typeof accountInfo.name !== 'undefined' && accountInfo.name !== ''">{{accountInfo.name}}</span>
                    <span v-if="typeof accountInfo.name !== 'undefined' && accountInfo.name !== ''">&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>{{$t('dialog.account_info_available_asset')}}&nbsp;</span><span>{{accountInfo.unconfirmedBalanceNQT/100000000}}&nbsp;SS</span>
                    <!--<span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span><span>{{$t('dialog.account_info_alias')}}&nbsp;</span><span></span>-->
                </div>
                <div class="account_allInfo">
                    <el-radio-group v-model="tabTitle" class="title">
                        <el-radio-button label="account" class="btn">{{$t('dialog.account_info_total_transaction')}}
                        </el-radio-button>
                    </el-radio-group>

                    <div v-if="tabTitle === 'account'" class="account_list">
                        <table class="table">
                            <tr>
                                <th>{{$t('dialog.account_info_transaction_time')}}</th>
                                <th>{{$t('dialog.account_info_transaction_type')}}</th>
                                <th>{{$t('dialog.account_info_amount')}}</th>
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
                                    <td>{{$global.myFormatTime(transactions.timestamp,'YMDHMS',true)}}</td>
                                    <td v-if="transactions.type === 0">
                                        <img src="../../assets/img/pay.svg"/>
                                        <span>{{$t('dialog.account_info_payment')}}</span>
                                    </td>
                                    <td v-else-if="transactions.type === 1 && transactions.subtype === 0">
                                        <img src="../../assets/img/infomation.svg"/>
                                        <span>{{$t('dialog.account_info_information')}}</span>
                                    </td>
                                    <td v-else-if="transactions.type === 1 && transactions.subtype === 5">
                                        <img src="../../assets/img/rename.svg"/>
                                        <span>{{$t('dialog.account_info_account_info')}}</span>
                                    </td>
                                    <td v-else-if="transactions.type === 6">
                                        <img src="../../assets/img/storage.svg"/>
                                        <span>{{$t('dialog.account_info_data_storage')}}</span>
                                    </td>
                                    <td v-else-if="transactions.type === 8">
                                        <img src="../../assets/img/forge_pool.svg"/>
                                        <span>{{$t('dialog.account_info_forge_pool')}}</span>
                                    </td>
                                    <td v-else-if="transactions.type === 9">
                                        <img src="../../assets/img/coinBase.svg"/>
                                        <span>CoinBase</span>
                                    </td>
                                    <td v-else-if="transactions.type === 12">
                                        <img src="../../assets/img/POC.svg"/>
                                        <span>POC交易</span>
                                    </td>
                                    <td>{{transactions.amountNQT/100000000}}</td>
                                    <td>{{transactions.feeNQT/100000000}}</td>
                                    <td class="linker w200" @click="checkAccountInfo(transactions.senderRS)"><span>{{transactions.senderRS}}</span>
                                    </td>
                                    <td class="linker" @click="openTransactionDialog(transactions.transaction)">
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
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.account_info_title1')}}{{accountInfo.accountRS}}{{$t('dialog.account_info_title2')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span>{{$t('dialog.account_info_name')}}&nbsp;</span><span></span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>{{$t('dialog.account_info_available_asset')}}&nbsp;</span><span>{{accountInfo.unconfirmedBalanceNQT/100000000}}&nbsp;SS</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>{{$t('dialog.account_info_alias')}}&nbsp;</span><span></span>
                </div>
                <div class="account_transactionInfo">
                    <p class="fl">{{$t('dialog.account_transaction_detail')}}</p>
                    <button class="fr common_btn" @click="openAccountInfo(accountInfo.accountRS)">
                        {{$t('dialog.account_transaction_return')}}
                    </button>
                    <div class="cb"></div>
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>{{$t('dialog.account_transaction_signature')}}</th>
                            <td>{{transactionInfo.signature}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_transaction_serial_number')}}</th>
                            <td v-if="typeof transactionInfo.transactionIndex !== 'undefined'">
                                {{transactionInfo.transactionIndex}}
                            </td>
                            <td v-else>-</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_type')}}</th>
                            <td v-if="transactionInfo.type === 0">
                                <span>{{$t('dialog.account_info_payment')}}</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 1&&transactionInfo.subtype === 0">
                                <span>{{$t('dialog.account_info_information')}}</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 1&&transactionInfo.subtype === 5">
                                <span>{{$t('dialog.account_info_account_info')}}</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 6">
                                <span>{{$t('dialog.account_info_data_storage')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 8">
                                <span>{{$t('transaction.transaction_type_forge_pool')}}</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 9">
                                <span>CoinBase</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 12">
                                <span>POC交易</span>
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_signatureHash')}}</th>
                            <td>{{transactionInfo.signatureHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_sender')}}</th>
                            <td v-if="transactionInfo.type === 9"></td>
                            <td v-else-if="$store.state.account !== transactionInfo.senderRS">
                                {{transactionInfo.senderRS}}
                            </td>
                            <td v-else-if="$store.state.account === transactionInfo.senderRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_amount')}}</th>
                            <td>{{transactionInfo.amountNQT/100000000}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                            <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">
                                {{transactionInfo.senderRS}}
                            </td>
                            <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                            <td v-else-if="$store.state.account === transactionInfo.recipientRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                            <td v-else-if="typeof transactionInfo.recipientRS === 'undefined'">-</td>
                            <td v-else-if="$store.state.account !== transactionInfo.recipientRS">
                                {{transactionInfo.recipientRS}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                            <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.blockTimestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.blockTimestamp,'YMDHMS',true)}}
                            </td>
                            <td v-else>-</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_timestamp')}}</th>
                            <td>{{transactionInfo.timestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.timestamp,'YMDHMS',true)}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_sender_public_key')}}</th>
                            <td>{{transactionInfo.senderPublicKey}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_info_fee')}}</th>
                            <td>{{transactionInfo.feeNQT/100000000}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_confirm')}}</th>
                            <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.confirmations}}
                            </td>
                            <td v-else>-</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_fullHash')}}</th>
                            <td>{{transactionInfo.fullHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_version')}}</th>
                            <td>{{transactionInfo.version}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_sender')}}</th>
                            <td v-if="transactionInfo.type === 9"></td>
                            <td v-else-if="$store.state.account !== transactionInfo.senderRS">
                                {{transactionInfo.sender}}
                            </td>
                            <td v-else-if="$store.state.account === transactionInfo.senderRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                            <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">
                                {{transactionInfo.senderRS}}
                            </td>
                            <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                            <td v-else-if="$store.state.account === transactionInfo.recipientRS">
                                {{$t('dialog.account_transaction_own')}}
                            </td>
                            <td v-else-if="typeof transactionInfo.recipientRS === 'undefined'">-</td>
                            <td v-else-if="$store.state.account !== transactionInfo.recipientRS">
                                {{transactionInfo.recipientRS}}
                            </td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_block_height')}}</th>
                            <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.height}}</td>
                            <td v-else>-</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
        <!--view block info-->
        <div class="modal_info" id="block_info" v-show="blockInfoDialog">

            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog()"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.block_info_title1')}}{{blockInfo.block}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <el-radio-group v-model="tabTitle" class="title">
                    <el-radio-button label="account" class="btn">{{$t('dialog.block_info_all_transaction')}}
                    </el-radio-button>
                    <el-radio-button label="blockInfo" class="btn">{{$t('dialog.block_info_all_block_detail')}}
                    </el-radio-button>
                    <el-radio-button label="pocInfo" class="btn">POC权重表</el-radio-button>
                </el-radio-group>

                <div v-if="tabTitle === 'account'" class="account_list">
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>{{$t('dialog.block_info_time')}}</th>
                            <th>{{$t('dialog.block_info_type')}}</th>
                            <th>{{$t('dialog.block_info_amount')}}</th>
                            <th>{{$t('dialog.account_info_fee')}}</th>
                            <th>{{$t('dialog.account_transaction_sender')}}</th>
                            <th>{{$t('dialog.account_transaction_recipient')}}</th>
                        </tr>
                        <tr v-for="(transaction,index) in blockInfo.transactions">
                            <td>{{$global.myFormatTime(transaction.timestamp,'YMDHMS',true)}}</td>
                            <td v-if="transaction.type === 0">
                                <img src="../../assets/img/pay.svg"/>
                                <span>{{$t('dialog.account_info_payment')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 1&&transaction.subtype === 0">
                                <img src="../../assets/img/infomation.svg"/>
                                <span>{{$t('dialog.account_info_information')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 1&&transaction.subtype === 5">
                                <img src="../../assets/img/rename.svg"/>
                                <span>{{$t('dialog.account_info_account_info')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 6">
                                <img src="../../assets/img/storage.svg"/>
                                <span>{{$t('dialog.account_info_data_storage')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 8">
                                <img src="../../assets/img/forge_pool.svg"/>
                                <span>{{$t('transaction.transaction_type_forge_pool')}}</span>
                            </td>
                            <td v-else-if="transaction.type === 9">
                                <img src="../../assets/img/coinBase.svg"/>
                                <span>CoinBase</span>
                            </td>
                            <td v-else-if="transaction.type === 12">
                                <img src="../../assets/img/POC.svg"/>
                                <span>{{$t("account.poc_trading")}}</span>
                            </td>
                            <td>{{transaction.amountNQT/100000000}}</td>
                            <td v-if="transaction.feeNQT">{{transaction.feeNQT/100000000}} SS</td>
                            <td v-else></td>
                            <td v-if="transaction.type === 9">CoinBase</td>
                            <td class="linker" v-else @click="openAccountInfo(transaction.senderRS)">
                                {{transaction.senderRS}}
                            </td>
                            <td class="linker" v-if="transaction.type === 9"
                                @click="openAccountInfo(transaction.senderRS)">{{transaction.senderRS}}
                            </td>
                            <td class="linker" v-else @click="openAccountInfo(transaction.recipientRS)">
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
                            <th>{{$t('dialog.block_info_previous_block_hash')}}</th>
                            <td>{{blockInfo.previousBlockHash}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_payload_length')}}</th>
                            <td>{{blockInfo.payloadLength}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_total_amount')}}</th>
                            <td>{{blockInfo.totalAmountNQT/100000000}} SS</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_generation_signature')}}</th>
                            <td>{{blockInfo.generationSignature}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_generation_public_key')}}</th>
                            <td>{{blockInfo.generatorPublicKey}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_transcation_amount')}}</th>
                            <td>{{blockInfo.numberOfTransactions}}</td>
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
                            <th>{{$t('dialog.block_info_total_fee')}}</th>
                            <td>{{blockInfo.totalFeeNQT/100000000}} SS</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_cumulative_difficulty')}}</th>
                            <td>{{blockInfo.cumulativeDifficulty}}</td>
                        </tr>
                        <tr>
                            <th>{{$t("account.block_number")}}</th>
                            <td>{{blockInfo.block}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_block_height')}}</th>
                            <td>{{blockInfo.height}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.account_transaction_timestamp')}}</th>
                            <td>{{blockInfo.timestamp}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('dialog.block_info_mining')}}</th>
                            <td class="linker" @click="openAccountInfo(blockInfo.generatorRS)">
                                {{blockInfo.generatorRS}}
                            </td>
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
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>bocSpeedTemplate</th>
                            <td>{{pocInfo.bocSpeedTemplate}}</td>
                        </tr>
                        <tr>
                            <th>generationMissingTemplate</th>
                            <td>{{pocInfo.generationMissingTemplate}}</td>
                        </tr>
                        <tr>
                            <th>hardwareConfigTemplate</th>
                            <td>{{pocInfo.hardwareConfigTemplate}}</td>
                        </tr>
                        <tr>
                            <th>networkConfigTemplate</th>
                            <td>{{pocInfo.networkConfigTemplate}}</td>
                        </tr>
                        <tr>
                            <th>nodeTypeTemplate</th>
                            <td>{{pocInfo.nodeTypeTemplate}}</td>
                        </tr>
                        <tr>
                            <th>onlineRateTemplate</th>
                            <td>{{pocInfo.onlineRateTemplate}}</td>
                        </tr>
                        <tr>
                            <th>serverOpenTemplate</th>
                            <td>{{pocInfo.serverOpenTemplate}}</td>
                        </tr>
                        <tr>
                            <th>templateVersion</th>
                            <td>{{pocInfo.templateVersion}}</td>
                        </tr>
                        <tr>
                            <th>txPerformanceTemplate</th>
                            <td>{{pocInfo.txPerformanceTemplate}}</td>
                        </tr>
                        <tr>
                            <th>version.pocWeightTable</th>
                            <td>{{pocInfo['version.pocWeightTable']}}</td>
                        </tr>
                        <tr>
                            <th>weightMap</th>
                            <td>{{pocInfo.weightMap}}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="trading_info" v-show="tradingInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('dialog.account_transaction_detail')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                    <tr>
                        <th>{{$t('dialog.account_transaction_signature')}}</th>
                        <td>{{transactionInfo.signature}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_transaction_serial_number')}}</th>
                        <td v-if="typeof transactionInfo.transactionIndex !== 'undefined'">
                            {{transactionInfo.transactionIndex}}
                        </td>
                        <td v-else>-</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_type')}}</th>
                        <td v-if="transactionInfo.type === 0">
                            <span>{{$t('dialog.account_info_payment')}}</span>
                        </td>
                        <td v-else-if="transactionInfo.type === 1&&transactionInfo.subtype === 0">
                            <span>{{$t('dialog.account_info_information')}}</span>
                        </td>
                        <td v-else-if="transactionInfo.type === 1&&transactionInfo.subtype === 5">
                            <span>{{$t('dialog.account_info_account_info')}}</span>
                        </td>
                        <td v-else-if="transactionInfo.type === 6">
                            <span>{{$t('dialog.account_info_data_storage')}}</span>
                        </td>
                        <td v-else-if="transactionInfo.type === 8">
                            <span>{{$t('dialog.account_info_block_reward')}}</span>
                        </td>
                        <td v-else-if="transactionInfo.type === 9">
                            <span>CoinBase</span>
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_signatureHash')}}</th>
                        <td>{{transactionInfo.signatureHash}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_sender')}}</th>
                        <td v-if="transactionInfo.type === 9"></td>
                        <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.senderRS}}
                        </td>
                        <td v-else-if="$store.state.account === transactionInfo.senderRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_amount')}}</th>
                        <td>{{transactionInfo.amountNQT/100000000}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_recipient')}}</th>
                        <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">
                            {{transactionInfo.senderRS}}
                        </td>
                        <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                        <td v-else-if="$store.state.account === transactionInfo.recipientRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                        <td v-else-if="typeof transactionInfo.recipientRS === 'undefined'">-</td>
                        <td v-else-if="$store.state.account !== transactionInfo.recipientRS">
                            {{transactionInfo.recipientRS}}
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_block_timestamp')}}</th>
                        <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.blockTimestamp}}&nbsp;&nbsp;|
                            &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.blockTimestamp,'YMDHMS',true)}}
                        </td>
                        <td v-else>-</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_timestamp')}}</th>
                        <td>{{transactionInfo.timestamp}}&nbsp;&nbsp;|
                            &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.timestamp,'YMDHMS',true)}}
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_sender_public_key')}}</th>
                        <td>{{transactionInfo.senderPublicKey}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_info_fee')}}</th>
                        <td>{{transactionInfo.feeNQT/100000000}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_confirm')}}</th>
                        <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.confirmations}}</td>
                        <td v-else>-</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_fullHash')}}</th>
                        <td>{{transactionInfo.fullHash}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_version')}}</th>
                        <td>{{transactionInfo.version}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_sender')}}</th>
                        <td v-if="transactionInfo.type === 9"></td>
                        <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.sender}}
                        </td>
                        <td v-else-if="$store.state.account === transactionInfo.senderRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_recipient')}}</th>
                        <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">
                            {{transactionInfo.sender}}
                        </td>
                        <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                        <td v-else-if="$store.state.account === transactionInfo.recipientRS">
                            {{$t('dialog.account_transaction_own')}}
                        </td>
                        <td v-else-if="typeof transactionInfo.recipientRS === 'undefined'">-</td>
                        <td v-else-if="$store.state.account !== transactionInfo.recipientRS">
                            {{transactionInfo.recipient}}
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('dialog.account_transaction_block_height')}}</th>
                        <td v-if="typeof transactionInfo.block !== 'undefined'">{{transactionInfo.height}}</td>
                        <td v-else>-</td>
                    </tr>
                    </tbody>
                </table>
            </div>

        </div>
    </div>
</template>

<script>

    export default {
        name: "dialog_all",
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
                pocInfo: {},
                tradingInfoDialog: this.tradingInfoOpen,
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
            }
        },
        created() {
            let _this = this;
            _this.$global.fetch("GET", {
                height: 0,
                includeTransactions: true
            }, "getBlock").then(res => {
                for (let t of res.transactions) {
                    if (t.type === 12) {
                        _this.pocInfo = t.attachment;
                        break;
                    }
                }
            });
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
                        line-height: 40px;
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
                        width: 160px;
                    }

                    td {
                        width: 1100px;
                        padding: 0 60px;
                        line-height: 40px;
                    }
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
                            line-height: 40px;
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
        top: 20px;

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
                        width: 1140px;
                        padding: 0 60px;
                        line-height: 40px;
                    }
                }
            }
        }
    }

    #trading_info {
        top: 80px;

        .table {
            td {
                width: 980px;
                padding: 0 60px;
            }

            th {
                width: 160px;
            }
        }
    }
</style>
