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
                    <span>{{$t('dialog.account_info_available_asset') + ': ' + $global.getAmountFormat(accountInfo.unconfirmedBalanceNQT)}}</span>
                </div>
                <div class="account_allInfo">
                    <el-radio-group v-model="tabTitle" class="title">
                        <el-radio-button label="account" class="btn">{{$t('dialog.account_info_total_transaction')}}
                        </el-radio-button>
                    </el-radio-group>

                    <div v-if="tabTitle === 'account'" class="account_list">
                        <table class="table">
                            <tr>
                                <th class="mobile-th">{{$t('dialog.account_transaction_time')}}</th>
                                <th class="pc-table">{{$t('dialog.account_transaction_id')}}</th>
                                <th class="mobile-th">{{$t('dialog.account_transaction_type')}}</th>
                                <th class="mobile-th">{{$t('dialog.account_transaction_amount')}}</th>
                                <th class="pc-table">{{$t('dialog.account_info_fee')}}</th>
                                <th class="pc-table">{{$t('dialog.account_info_account')}}</th>
                                <th class="pc-table">{{$t('dialog.account_info_operating')}}</th>
                                <th class="gutter pc-table"></th>
                                <th class="mobile" style="width: 20px"></th>
                            </tr>
                        </table>
                        <div id="account_info_table_body" class="table_body">
                            <table class="table">
                                <tbody>
                                <tr v-for="transactions in accountTransactionInfo" @click="openTransactionDialogMobile(transactions.transaction)">
                                    <td class="mobile-td compact-style">
                                        <span class="compact-hidden">{{$global.myFormatTime(transactions.timestamp,'YMDHMS',true)}}</span><br>
                                        <span class="utc-time">{{$global.formatTime(transactions.timestamp)}} +UTC</span>
                                    </td>
                                    <td class="pc-table">{{transactions.transaction}}</td>
                                    <td class="transaction-img mobile-td">
                                        <span class="bg"
                                              :class="'type' + transactions.type + transactions.subtype"></span>
                                        <span>{{$global.getTransactionTypeStr(transactions)}}</span>
                                    </td>
                                    <td class="mobile-td">{{$global.getTransactionAmountNQT(transactions,accountInfo.accountRS)}}</td>
                                    <td class="pc-table">{{$global.getTransactionFeeNQT(transactions)}}</td>
                                    <td class="linker pc-table" style="font-size:11px;" @click="checkAccountInfo(transactions.senderRS)">
                                        <span>{{transactions.senderRS}}</span>
                                    </td>
                                    <td class="linker pc-table" style="font-size:11px;" @click="openTransactionDialog(transactions.transaction)">
                                        {{$t('dialog.account_info_view_detail')}}
                                    </td>
                                    <td class="mobile icon-box" style="width: 20px"  @click="openTransactionDialog(transaction.transaction)"><i class="el-icon-arrow-right"></i></td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="list_pagination"> <!--v-if="totalSize > pageSize">-->
                            <div class="list_pagination">
                                <el-pagination
                                    :small="isMobile"
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
                    <span>{{$t('dialog.account_info_available_asset') + ": " + $global.getAmountFormat(accountInfo.unconfirmedBalanceNQT)}}</span>
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
                        {{$t('dialog.block_info_title1')}} - {{blockInfo.block}} |<br class="mobile"> {{$t('dialog.account_transaction_block_height')}} - {{blockInfo.height}}
                    </span>
                </h4>
            </div>
            <div class="modal-body">
                <el-radio-group v-model="tabTitle" class="title">
                    <el-radio-button label="account" class="btn">{{$t('dialog.block_info_all_transaction')}}</el-radio-button>
                    <el-radio-button label="blockInfo" class="btn">{{$t('dialog.block_info_all_block_detail')}}</el-radio-button>
                    <el-radio-button label="blockRewardInfo" class="btn">{{$t('dialog.block_reward_distribution_detail')}}</el-radio-button>
                    <el-radio-button v-if="pocInfoList.length > 0" label="pocInfo" class="btn">PoC</el-radio-button>
                    <el-radio-button v-if="poolInfoList.length > 0" label="poolInfo" class="btn">Pool</el-radio-button>
                    <el-radio-button v-if="messageInfoList.length > 0" label="messageInfo" class="btn">{{$t('sendMessage.infomation')}}</el-radio-button>
                    <el-radio-button v-if="storageFileInfo.length > 0 && typeof(secretPhrase) !== 'undefined'" label="storageFileInfo" class="btn">{{$t('transaction.transaction_type_storage_service')}}</el-radio-button>
                </el-radio-group>

                <div v-if="tabTitle === 'account'" class="account_list">
                    <table  class="table">
                        <tbody>
                        <tr>
                            <th class="mobile-th">{{$t('dialog.account_transaction_time')}}</th>
                            <th class="pc-table">{{$t('dialog.account_transaction_id')}}</th>
                            <th class="mobile-th">{{$t('dialog.account_transaction_type')}}</th>
                            <th class="mobile-th">{{$t('dialog.account_transaction_amount')}}</th>
                            <th class="pc-table">{{$t('dialog.account_info_fee')}}</th>
                            <th class="pc-table">{{$t('dialog.account_transaction_sender')}}</th>
                            <th class="pc-table">{{$t('dialog.account_transaction_recipient')}}</th>
                            <th class="mobile" style="width: 20px"></th>
                        </tr>
                        <tr v-for="(transaction,index) in blockInfo.transactions">
                            <td class="mobile-th compact-style">
                                <span class="compact-hidden">{{$global.myFormatTime(transaction.timestamp,'YMDHMS',true)}}</span><br>
                                <span class="utc-time compact-small-font">{{$global.formatTime(transaction.timestamp)}} +UTC</span>
                            </td>
                            <td class="linker pc-table" @click="openTransactionDialog(transaction.transaction)">{{transaction.transaction}}</td>
                            <td class="transaction-img mobile-td compact-style" v-if="$global.projectName === 'mw'">
                                <span class="bg" :class="'type' + transaction.type + transaction.subtype"></span>
                                <span>{{$global.getTransactionTypeStr(transaction)}}</span>
                            </td>
                            <td class="transaction-img-sharder mobile-td compact-style" v-else-if="$global.projectName === 'sharder'">
                                <span class="bg" :class="'type' + transaction.type + transaction.subtype"></span>
                                <span>{{$global.getTransactionTypeStr(transaction)}}</span>
                            </td>
                            <td class="mobile-th">{{$global.getTransactionAmountNQT(transaction,"")}}</td>
                            <td class="pc-table">{{$global.getTransactionFeeNQT(transaction)}}</td>
                            <td class="pc-table" v-if="transaction.type === 9">CoinBase</td>
                            <td class="pc-table" v-else-if="transaction.type === 12">System</td>
                            <td class="linker pc-table" style="font-size:11px;" v-else @click="openAccountInfo(transaction.senderRS)">
                                {{transaction.senderRS}}
                            </td>
                            <td class="linker pc-table" style="font-size:11px;" v-if="transaction.type === 9"
                                @click="openAccountInfo(transaction.senderRS)">
                                {{transaction.senderRS}}
                            </td>
                            <td class="linker pc-table" style="font-size:11px;" v-else @click="openAccountInfo(transaction.recipientRS)">
                                {{transaction.recipientRS}}
                            </td>
                            <td class="mobile icon-box" style="width: 20px" @click="openTransactionDialog(transaction.transaction)"><i class="el-icon-arrow-right"></i></td>
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
                            <td>{{$global.getFormattedTimestamp(blockInfo.timestamp)}}</td>
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
                <!-- block reward distribution section -->
                <div v-if="tabTitle === 'blockRewardInfo'" class="account_list">
                    <!-- tabs -->
                    <el-radio-group v-model="rewardTabs" class="title">
                        <el-radio-button label="miningRewards" class="reward-tab">{{$t('dialog.block_reward_distribution_mining')}}</el-radio-button>
                        <el-radio-button v-if="containCrowdRewardTxs()" label="crowdMinerRewards" class="reward-tab">{{$t('dialog.block_reward_distribution_crowd')}}</el-radio-button>

                    </el-radio-group>

                    <div v-if="(rewardTabs === 'miningRewards') && containMiningRewardTxs()">
                        <p class="testnet-tips">{{$t('dialog.block_reward_distribution_mining')}}: {{$global.getAmountFormat(coinBaseTx.attachment.blockMiningRewardAmount)}}</p>
                    </div>
                    <div v-else-if="(rewardTabs === 'crowdMinerRewards') && containCrowdRewardTxs()">
                        <p class="testnet-tips">{{$t('dialog.block_reward_distribution_crowd')}}: {{$global.getAmountFormat(coinBaseTx.attachment.crowdMinerRewardAmount)}} / {{coinBaseTx.attachment.crowdMiners.length}} {{$t('dialog.block_reward_miners')}} - <span v-if="blockInfo.hasRewardDistribution" style="color: #00ff99">{{$t('network.crowd_miner_reward_success')}}</span><span v-if="!blockInfo.hasRewardDistribution" style="color: red">{{$t('network.crowd_miner_reward_fail')}}</span></p>
                    </div>

                    <!-- mining rewards(include pool mode) distribution table -->
                    <table v-if="(rewardTabs === 'miningRewards')" class="table">
                        <tbody>
                        <tr>
                            <th class="pc-table">{{$t('dialog.account_info_account_id')}}</th>
                            <th class="mobile-th">{{$t('dialog.block_info_mining')}}</th>
                            <th class="mobile-th">{{$t('dialog.account_info_staking_amount')}}</th>
                            <th class="mobile-th">{{$t('dialog.account_transaction_amount')}}</th>
                            <th class="pc-table">{{$t('dialog.account_transaction_sender')}}</th>
                            <th class="mobile" style="width: 20px"></th>
                        </tr>
                        <tr v-if="containMiningRewardTxs()">
                            <template v-for="(poolJoiner,index) in coinBaseTx.attachment.consignors">
                                <td class="linker pc-table" >{{poolJoiner.accountId}}</td>
                                <td class="linker mobile-td compact-style" >{{poolJoiner.accountRS}}</td>
                                <td class="pc-table mobile-td compact-style">{{$global.getAmountFormat(poolJoiner.investAmount)}}</td>
                                <td class="pc-table mobile-td compact-style">{{$global.getAmountFormat(poolJoiner.rewardAmount)}}</td>
                                <td class="pc-table">CoinBase</td>
                                <td class="mobile icon-box" style="width: 20px"><i class="el-icon-arrow-right"></i></td>
                            </template>
                        </tr>
                        <tr v-else>
<!--                            <td class="linker pc-table" >{{coinBaseTx.sender}}</td>-->
<!--                            <td class="linker mobile-td compact-style" >{{coinBaseTx.senderRS}}</td>-->
                            <td class="linker pc-table" >{{blockInfo.generator}}</td>
                            <td class="linker mobile-td compact-style" >{{blockInfo.generatorRS}}</td>
                            <td class="mobile-td compact-style">--</td>
                            <td class="mobile-td compact-style">{{$global.getAmountFormat(coinBaseTx.attachment.blockMiningRewardAmount)}}</td>
                            <td class="pc-table">CoinBase</td>
                            <td class="mobile icon-box" style="width: 20px"><i class="el-icon-arrow-right"></i></td>
                        </tr>
                        </tbody>
                    </table>

                    <!-- crowd miners rewards distribution table -->
                    <table v-if="(rewardTabs === 'crowdMinerRewards') && containCrowdRewardTxs()" class="table">
                        <tbody>
                            <tr>
                                <th class="pc-table">{{$t('dialog.account_info_account_id')}}</th>
                                <th class="mobile-th compact-style">{{$t('dialog.block_info_mining')}}</th>
                                <th class="mobile-th compact-style">{{$t('dialog.account_info_poc_score')}}</th>
                                <th class="mobile-th compact-style">{{$t('dialog.account_transaction_amount')}}</th>
                                <th class="pc-table">{{$t('dialog.account_transaction_sender')}}</th>
                                <th class="mobile" style="width: 20px"></th>
                            </tr>
                            <tr v-for="(crowdMiner,index) in coinBaseTx.attachment.crowdMiners">
                                <td class="linker pc-table" >{{crowdMiner.accountId}}</td>
                                <td class="linker mobile-td compact-style" >{{crowdMiner.accountRS}}</td>
                                <td class="mobile-td compact-style" v-if="crowdMiner.pocScore === -1">--</td>
                                <td class="mobile-td compact-style" v-else>{{crowdMiner.pocScore}}</td>
                                <td class="mobile-td compact-style">{{$global.getAmountFormat(crowdMiner.rewardAmount)}}</td>
                                <td class="pc-table">CoinBase</td>
                                <td class="mobile icon-box" style="width: 20px"><i class="el-icon-arrow-right"></i></td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div v-if="tabTitle === 'pocInfo'" class="blockInfo">
                    <!-- pc -->
                    <el-table :data="pocInfoList" class="poc pc" style="width: 100%">
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

                    <!-- mobile -->
                    <el-table :data="pocInfoList" class="poc mobile" style="width: 100%">
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
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                    </el-table>
                </div>
                <div v-if="tabTitle === 'poolInfo'" class="blockInfo">
                    <!-- pc -->
                    <el-table :data="poolInfoList" class="poc pc" style="width: 100%">
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

                    <!-- mobile -->
                    <el-table :data="poolInfoList" class="poc mobile" style="width: 100%">
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
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                    </el-table>
                </div>
                <div v-if="tabTitle === 'messageInfo'" class="blockInfo">
                    <!-- pc -->
                    <el-table :data="messageInfoList" class="poc pc" style="width: 100%">
                        <el-table-column type="expand">
                            <template slot-scope="props">
                                <el-form label-position="left" inline>
                                    <el-row>
                                        <MessageTxDetail :rowData="props.row"></MessageTxDetail>
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
                            align="center"
                            :label="$t('poc.type')"
                            :formatter="parseSubTypeMessage">
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

                    <!-- mobile -->
                    <el-table :data="messageInfoList" class="poc mobile" style="width: 100%">
                        <el-table-column type="expand">
                            <template slot-scope="props">
                                <el-form label-position="left" inline>
                                    <el-row>
                                        <MessageTxDetail :rowData="props.row"></MessageTxDetail>
                                    </el-row>
                                </el-form>
                            </template>
                        </el-table-column>

                        <el-table-column
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                    </el-table>
                </div>
                <div v-if="tabTitle === 'storageFileInfo'" class="blockInfo">
                    <el-table :data="storageFileInfo" class="poc" style="width: 100%">
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
                            prop="transaction"
                            align="center"
                            :label="$t('poc.tx')">
                        </el-table-column>
                        <el-table-column
                            align="center"
                            :label="$t('network.block_list_operating')">
                            <template slot-scope="scope">
                                <el-button @click="downloadFile(scope.row)">{{$t('network.download')}}</el-button>
                            </template>
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
                <el-button v-show="isShowMore" id="findTXInHecoChain"
                           @click="findTXInHecoChain(transactionInfo.fullHash)">
                    {{$t('acrossChains.tx_in_chain')}}
                </el-button>
            </div>
        </div>
    </div>
</template>

<script>
    import MessageTxDetail from "./MessageTxDetail";
    export default {
        name: "dialog_all",
        components:{MessageTxDetail},
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
                rewardTabs: 'miningRewards',
                accountInfoDialog: this.accountInfoOpen,
                accountInfo: [],
                accountTransactionInfo: [],
                accountTransactionDialog: false,
                transactionInfo: [],
                transaction: '',
                accountRS: this.generatorRS,
                searchVal: '',
                blockInfoDialog: this.blockInfoOpen,
                coinBaseTx: '',
                blockInfo: [],
                pocInfoList:[],
                poolInfoList:[],
                messageInfoList:[],
                accountIdMap:[],
                storageFileInfo:[],
                tradingInfoDialog: this.tradingInfoOpen,
                rs:'',
                secretPhrase:SSO.secretPhrase,
                pageNO: 1,
                isMobile: false,
                totalSize: 0,
                pageSize: 10,
                currentPage: 1,
                accountBind: null,

                acrossChains: {
                    Heco: {
                        CosExchangeAddress: "CDW-XXXX-XXXX-XXXX-Heco",
                    },
                    OKEx: {
                        CosExchangeAddress: "CDW-XXXX-XXXX-XXXX-OKEx",
                    },
                    ETH: {
                        CosExchangeAddress: "CDW-XXXX-XXXX-XXXX-ETH",
                    },
                    Tron: {
                        CosExchangeAddress: "CDW-XXXX-XXXX-XXXX-Tron",
                    },
                    BSC: {
                        CosExchangeAddress: "CDW-XXXX-XXXX-XXXX-BSC",
                    },
                },

                chainId:1,
                isShowMore:false,
                recordType:1,
            }
        },
        created() {
            if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                this.isMobile = true
            }
            this.getAcrossAddress();
        },
        methods: {
            handleSizeChange(val) {

            },
            handleCurrentChange(val) {
                console.log(`当前页: ${val}`);
                console.log(`当前账户: ${this.accountBind}`);
                this.currentPage = val;
                this.httpGetAccountInfo(this.accountBind);
            },
            containCrowdRewardTxs() {
                const _this = this;

                if(_this.coinBaseTx !== ''
                && _this.coinBaseTx.attachment.crowdMiners
                && _this.coinBaseTx.attachment.crowdMiners !== '{}'
                && _this.coinBaseTx.attachment.crowdMiners !== '[]'
                && _this.coinBaseTx.attachment.crowdMiners.length > 0){
                return true;
                }

                return false;
            },
            containMiningRewardTxs() {
                const _this = this;

                if(_this.coinBaseTx !== ''
                && _this.coinBaseTx.attachment.consignors
                && _this.coinBaseTx.attachment.consignors !== '{}'
                && _this.coinBaseTx.attachment.consignors !== '[]'
                && _this.coinBaseTx.attachment.consignors.length > 0){
                    return true;
                }
                return false;
            },

            httpGetAccountInfo(accountID) {
                const _this = this;
                _this.accountBind = accountID;
                return new Promise((resolve, reject) => {

                    _this.$http.get(_this.$global.urlPrefix() + '?requestType=getAccount', {
                        params: {
                            account: accountID
                        }
                    }).then(function (res) {
                        if (!res.data.errorDescription) {
                            _this.accountInfo = res.data;
                            let params = new URLSearchParams();
                            params.append("account", accountID);
                            params.append("firstIndex", (_this.currentPage - 1) * 10);
                            params.append("lastIndex", (_this.currentPage - 1) * 10 + 9);
                            _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactions', {params}).then(function (res) {
                                _this.accountTransactionInfo = res.data.transactions;
                                _this.totalSize = res.data.count;
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
                _this.messageInfoList = [];
                _this.storageFileInfo =[];
                return new Promise((resolve, reject) => {
                    _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlock', {
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
                                        heightandblock:t.height+ " / " +t.block,
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
                                }else if (t.type === 9) {
                                    _this.coinBaseTx=t;
                                    console.log('coinBaseTx')
                                    console.log(_this.coinBaseTx)
                                }else if(t.type === 1){
                                    _this.messageInfoList.push({
                                        messageInfo:t.attachment,
                                        senderRS:t.senderRS,
                                        block:t.block,
                                        height:t.height,
                                        transaction:t.transaction,
                                        subType:t.subtype,
                                        recipientRS:t.recipientRS,
                                        accountRS:SSO.accountRS,
                                    });
                                }else if(t.type === 11){
                                    _this.storageFileInfo.push({
                                        fileInfo:t.attachment,
                                        senderRS:t.senderRS,
                                        block:t.block,
                                        height:t.height,
                                        transaction:t.transaction,
                                        type:t.type,
                                        feeNQT:t.feeNQT,
                                    });
                                }
                            }
                            let formData = new FormData();
                            formData.append("accoutId", accoutIdArray.join(","));
                            _this.loading = true;
                            _this.$http.post(_this.$global.urlPrefix() + '?requestType=getAccountId', formData).then(function (res) {
                                if (!res.data.errorDescription) {
                                    _this.accountIdMap = res.data.rsAccountInfo;
                                    console.log("map:"+_this.accountIdMap);
                                } else {
                                    console.log(res.data.errorDescription);
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
                    this.$http.get(_this.$global.urlPrefix() + '?requestType=getTransaction', {
                        params: {
                            transaction: tradingID
                        }
                    }).then(function (res) {
                        if (!res.data.errorDescription) {
                            _this.transactionInfo = res.data;
                            resolve("success");

                            if(_this.$global.getSenderOrRecipient(_this.transactionInfo) === _this.acrossChains.Heco.CosExchangeAddress){
                                _this.chainId = 1;
                                _this.isShowMore = true;
                                _this.recordType = 1;
                            }else if(_this.$global.getSenderRSOrWo(_this.transactionInfo) === _this.acrossChains.Heco.CosExchangeAddress){
                                _this.chainId = 1;
                                _this.isShowMore = true
                                _this.recordType = 2;
                            }else if(_this.$global.getSenderOrRecipient(_this.transactionInfo) === _this.acrossChains.OKEx.CosExchangeAddress){
                                _this.chainId = 2;
                                _this.isShowMore = true
                                _this.recordType = 1;
                            }else if(_this.$global.getSenderRSOrWo(_this.transactionInfo) === _this.acrossChains.OKEx.CosExchangeAddress){
                                _this.chainId = 2;
                                _this.isShowMore = true
                                _this.recordType = 2;
                            }else if(_this.$global.getSenderOrRecipient(_this.transactionInfo) === _this.acrossChains.ETH.CosExchangeAddress){
                                _this.chainId = 3;
                                _this.isShowMore = true
                                _this.recordType = 1;
                            }else if(_this.$global.getSenderRSOrWo(_this.transactionInfo) === _this.acrossChains.ETH.CosExchangeAddress){
                                _this.chainId = 3;
                                _this.isShowMore = true
                                _this.recordType = 2;
                            }else if(_this.$global.getSenderOrRecipient(_this.transactionInfo) === _this.acrossChains.Tron.CosExchangeAddress){
                                _this.chainId = 4;
                                _this.isShowMore = true
                                _this.recordType = 1;
                            }else if(_this.$global.getSenderRSOrWo(_this.transactionInfo) === _this.acrossChains.Tron.CosExchangeAddress){
                                _this.chainId = 4;
                                _this.isShowMore = true
                                _this.recordType = 2;
                            }else if(_this.$global.getSenderOrRecipient(_this.transactionInfo) === _this.acrossChains.BSC.CosExchangeAddress){
                                _this.chainId = 5;
                                _this.isShowMore = true
                                _this.recordType = 1;
                            }else if(_this.$global.getSenderRSOrWo(_this.transactionInfo) === _this.acrossChains.BSC.CosExchangeAddress){
                                _this.chainId = 5;
                                _this.isShowMore = true
                                _this.recordType = 2;
                            }
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
            openTransactionDialogMobile(transaction) {
                if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                    this.openTransactionDialog(transaction)
                }
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
                let nodeType = row.pocInfo.type;
                let txSubType = row.subType;
/*                console.info("type:"+type);*/
                if(txSubType === 2){
                    return '--';
                }else{
                    switch (nodeType) {
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
                            // return this.$root.$t("poc.normal_node");
                            return '--';
                    }
                }
            },
            parseSubTypePool(row, column) {
                let subtype = row.subType;
                if(row.type === 11) return  this.$root.$t("transaction.transaction_type_storage_service");
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
            parseSubTypeMessage(row,column) {
                const _this = this;
                let subtype = row.subType;
                if (subtype === 0) return _this.$root.$t("transaction.transaction_type_information");
                if (subtype === 5) return _this.$root.$t("transaction.transaction_type_account");
            },
            downloadFile(row,column){
                // window.open(this.$global.urlPrefix() + "?requestType=downloadStoredData&ssid="+row.fileInfo.ssid+"&filename="+row.fileInfo.name,"_blank")
                const img = new Image;
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                img.onload = function() {
                    canvas.width = this.width;
                    canvas.height = this.height;
                    ctx.drawImage(this, 0, 0);

                    const elt = document.createElement('a');
                    elt.setAttribute('href', canvas.toDataURL('image/png'));
                    elt.setAttribute('download', row.fileInfo.name);
                    elt.style.display = 'none';
                    document.body.appendChild(elt);
                    elt.click();
                    document.body.removeChild(elt);
                };
                img.crossOrigin = 'anonymous';
                img.src = window.api.apiUrl + this.$global.urlPrefix() + "?requestType=downloadStoredData&ssid="+row.fileInfo.ssid+"&filename="+row.fileInfo.name;
            },
            getAcrossAddress(){
                const _this = this;
                if(!this.$global.acrossChainExchangeEnable){
                    return;
                }
                _this.$http.get(window.api.getAcrossAddress).then(function (res1) {
                    var result = res1.data.body;
                    if (result) {
                        _this.acrossChains.Heco.CosExchangeAddress = result.Heco.CosExchangeAddress;
                        _this.acrossChains.OKEx.CosExchangeAddress = result.OKEx.CosExchangeAddress;
                        _this.acrossChains.ETH.CosExchangeAddress = result.ETH.CosExchangeAddress;
                        _this.acrossChains.Tron.CosExchangeAddress = result.Tron.CosExchangeAddress;
                        _this.acrossChains.BSC.CosExchangeAddress = result.BSC.CosExchangeAddress;
                    }
                });
            },
            findTXInHecoChain(fullHash){
                const _this = this;
                _this.$http.get(window.api.getRecordUrl,{params:{fullSource:fullHash,recordType:_this.recordType}}).then(function (res1) {
                    var tx = res1.data.body.transactionHash;
                    if(tx){
                        switch(_this.chainId){
                            case 1:
                                window.open(window.api.getHecoInfo+tx, '_blank');
                                break;
                            case 2:
                                window.open(window.api.getOKExInfo+tx, '_blank');
                                break;
                            case 3:
                                window.open(window.api.getETHInfo+tx, '_blank');
                                break;
                            case 4:
                                window.open(window.api.getTronInfo+tx, '_blank');
                                break;
                            case 5:
                                window.open(window.api.getBSCInfo+tx, '_blank');
                                break;
                            default:
                                break;
                        }
                    }else{
                        _this.$message.error(_this.$t('acrossChains.tx_error'));
                    }
                }).catch(err => {
                    _this.$message.error(_this.$t('acrossChains.error'));
                });


            }

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
@import '../../styles/css/vars.scss';
    .is-active /deep/.el-radio-button__inner {
        border-bottom: 2px solid $primary_color !important;
    }

    .reward-tab /deep/.el-radio-button__inner {
        background: #fff !important;
        color: #333 !important;
        margin-top: 5px !important;
        border-radius: 0 !important;;
        border: 0;
        box-shadow: 0 0 0 0 $primary_color;
    }

    #block_info {
        .modal-body {
            max-height: 600px;
            overflow-y: auto;
            margin: 30px 20px 40px !important;

            .title {
                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: $primary_color;
                }

                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }

                .el-radio-button__inner:hover {
                    color: $primary_color;
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
                        color: $primary_color;
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
                    color: #555;
                }
            }

            .account_allInfo {
                margin: 20px;

                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: $primary_color;
                }

                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }

                .el-radio-button__inner:hover {
                    color: $primary_color;
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
                            color: $primary_color;
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
                    color: #555;
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

    .compact-style {
        text-align: center!important;
        text-indent: 5px!important;
    }

    .compact-small-font {
        font-size: 9px;
    }


    @media only screen and (max-width: 780px) {
        #account_info .modal-body .account_allInfo .account_list .table td {
            width: 300px;
        }
        #account_info .modal-body .account_allInfo .account_list .table_body {
            max-height: 300px;
        }

        .compact-hidden {
            display: none;
        }
    }

    #findTXInHecoChain{
        color: #FFF;
        background: #3fb09a;
        margin-top: 20px;
    }

</style>
