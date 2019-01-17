<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
    <div>
        <div>
            <div class="block_account mb20">
                <p class="block_title">
                    <img src="../../assets/img/account.svg"/>
                    <span>{{$t('account.account_title')}}</span>
                </p>
                <div class="w pt60">
                    <div class="account_address">
                        <span>{{accountInfo.accountRS}}</span>
                        <img class="csp" src="../../assets/img/copy.svg" v-clipboard:copy="accountInfo.accountRS"
                             v-clipboard:success="copySuccess" v-clipboard:error="copyError"/>
                        <span class="csp" @click="openUserInfoDialog">{{$t('account.account_info')}}</span>
                    </div>
                    <p class="account_asset">{{$t('account.assets')}}{{$global.formatMoney(accountInfo.unconfirmedBalanceNQT/100000000, 8)}} SS</p>
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
                            <span>{{$t('account.transfer')}}</span>
                        </button>
                        <button class="common_btn imgBtn" @click="openSendMessageDialog">
                            <span class="icon">
                                <svg fill="#fff" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 171.43 137.08">
                                    <path class="cls-1"
                                          d="M173.41,143.1a10.26,10.26,0,0,1-10.24,10.25H36.84A10.26,10.26,0,0,1,26.59,143.1v-92A10.25,10.25,0,0,1,36.84,40.88H163.16a10.25,10.25,0,0,1,10.24,10.24v92ZM163.16,28.57H36.84A22.57,22.57,0,0,0,14.29,51.12v92a22.57,22.57,0,0,0,22.55,22.55H163.16a22.57,22.57,0,0,0,22.55-22.55v-92A22.57,22.57,0,0,0,163.16,28.57ZM151.88,65L100,94.89,47.26,65a6.31,6.31,0,0,0-8.39,2.31,6.16,6.16,0,0,0,2.31,8.39L100,109.07l58-33.44a6.16,6.16,0,0,0,2.26-8.41,6.33,6.33,0,0,0-8.4-2.25h0Z"
                                          transform="translate(-14.29 -28.57)"/>
                                </svg>
                            </span>
                            <span>{{$t('account.send_message')}}</span>
                        </button>
                        
                        <button class="common_btn imgBtn" v-if="typeof(secretPhrase) !== 'undefined' && hubsetting.SS_Address === accountInfo.accountRS && !initHUb" @click="openHubSettingDialog">
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
                            <span>{{$t('account.hub_setting')}}</span>
                        </button>
                        <button class="common_btn imgBtn" v-if="typeof(secretPhrase) !== 'undefined' && initHUb" @click="openHubInitDialog">
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
                            <span>{{$t('login.init_hub')}}</span>
                        </button>
                    </div>
                </div>
            </div>
            <div class="block_receiptDisbursement mb20">
                <p class="block_title">
                    <img src="../../assets/img/receipt&disbursementInfo.svg"/>
                    <span>{{$t('account.income_and_expenditure_details')}}</span>
                </p>
                <div class="w">
                    <div class="whf" id="transaction_amount_bar">
                    </div>
                    <div class="whf" id="yield_curve">
                    </div>
                </div>
            </div>
            <div class="block_list">
                <p class="block_title fl">
                    <img src="../../assets/img/transaction.svg"/>
                    <span>{{$t('transaction.transaction_record')}}</span>
                </p>
                <div class="transaction_type">
                    <el-select v-model="selectType" :placeholder="$t('transaction.transaction_type_all')">
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
                                <th>{{$t('transaction.transaction_time')}}</th>
                                <th>{{$t('transaction.block_height')}}</th>
                                <th>{{$t('transaction.transaction_type')}}</th>
                                <th>{{$t('transaction.transaction_amount')}}</th>
                                <th>{{$t('transaction.transaction_fee')}}</th>
                                <th class="dbw w300">{{$t('transaction.transaction_account')}}</th>
                                <th>{{$t('transaction.transaction_confirm_quantity')}}</th>
                                <th>{{$t('transaction.operating')}}</th>
                            </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(transaction,index) in accountTransactionList" v-if="index>=(currentPage-1)*pageSize && index <= currentPage*pageSize -1">
                                    <td>{{$global.myFormatTime(transaction.timestamp, 'YMDHMS')}}</td>
                                    <td class="linker" @click="openBlockInfoDialog(transaction.height)" v-if="typeof transaction.block !== 'undefined'">{{transaction.height}}</td>
                                    <td class="linker" @click="openBlockInfoDialog(transaction.height)" v-else>-</td>
                                    <td v-if="transaction.type === 0">{{$t('transaction.transaction_type_payment')}}</td>
                                    <td v-if="transaction.type === 1 && transaction.subtype === 0">{{$t('transaction.transaction_type_information')}}</td>
                                    <td v-if="transaction.type === 1 && transaction.subtype === 5">{{$t('transaction.transaction_type_account')}}</td>
                                    <td v-if="transaction.type === 6">{{$t('transaction.transaction_type_storage_service')}}</td>
                                    <td v-if="transaction.type === 8">{{$t('transaction.transaction_type_forge_pool')}}</td>
                                    <td v-if="transaction.type === 9">{{$t('transaction.transaction_type_block_reward')}}</td>

                                    <td v-if="transaction.amountNQT === '0'">0 SS</td>
                                    <td v-else-if="transaction.senderRS === accountInfo.accountRS && transaction.type !== 9">-{{$global.formatMoney(transaction.amountNQT/100000000)}} SS</td>
                                    <td v-else>+{{$global.formatMoney(transaction.amountNQT/100000000)}} SS</td>

                                    <td>{{$global.formatMoney(transaction.feeNQT/100000000)}} SS</td>
                                    <td class=" image_text w300">
                                        <span class="linker" v-if="transaction.type === 9">Coinbase</span>
                                        <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)"
                                              v-else-if="transaction.senderRS === accountInfo.accountRS && transaction.type !== 9">{{$t('transaction.self')}}</span>
                                        <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)"
                                              v-else-if=" transaction.senderRS !== accountInfo.accountRS && transaction.type !== 9">{{transaction.senderRS}}</span>
                                        <img src="../../assets/img/right_arrow.svg"/>
                                        <span class="linker" @click="openAccountInfoDialog(transaction.senderRS)" v-if="transaction.type === 9">{{$t('transaction.self')}}</span>
                                        <span class="linker" @click="openAccountInfoDialog(transaction.recipientRS)"
                                              v-else-if="transaction.recipientRS === accountInfo.accountRS && transaction.type !== 9">{{$t('transaction.self')}}</span>
                                        <span class="linker" v-else-if="typeof transaction.recipientRS === 'undefined'">/</span>
                                        <span class="linker" @click="openAccountInfoDialog(transaction.recipientRS)"
                                              v-else-if="transaction.recipientRS !== accountInfo.accountRS && transaction.type !== 9">{{transaction.recipientRS}}</span>

                                    </td>
                                    <td  v-if="typeof transaction.block !== 'undefined'">{{transaction.confirmations}}</td>
                                    <td  v-else>-</td>
                                    <td class="linker" @click="openTradingInfoDialog(transaction.transaction)">{{$t('transaction.view_details')}}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination"> <!--v-if="totalSize > pageSize">-->
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
                        <h4 class="modal-title">{{$t('sendMessage.sendMessage_title')}}</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item :label="$t('sendMessage.receiver')" class="item_receiver">
                                <masked-input id="receiver" mask="AAA-****-****-****-*****" v-model="messageForm.receiver" />
                                <img src="../../assets/img/account_directory.svg"/>
                            </el-form-item>
                            <el-form-item :label="$t('sendMessage.receiver_publickey')" v-if="messageForm.hasPublicKey">
                                <el-input v-model="messageForm.publicKey" type="password"></el-input>
                            </el-form-item>
                            <el-form-item :label="$t('sendMessage.infomation')">
                                <el-checkbox v-model="messageForm.isEncrypted">{{$t('sendMessage.encrypted_information')}}</el-checkbox>
                                <el-input
                                    :disabled="messageForm.isFile"
                                    type="textarea"
                                    :autosize="{ minRows: 2, maxRows: 10}"
                                    resize="none"
                                    :placeholder="$t('sendMessage.message_tip')"
                                    v-model="messageForm.message">
                                </el-input>
                            </el-form-item>
                            <el-form-item :label="$t('sendMessage.file')">
                                <el-input :placeholder="$t('sendMessage.file_tip')" class="input-with-select" v-model="messageForm.fileName" :readonly="true">
                                    <el-button slot="append" v-if="file === null">{{$t('sendMessage.browse')}}</el-button>
                                    <el-button slot="append" @click="delFile" v-else>{{$t('sendMessage.delete')}}</el-button>
                                </el-input>
                                <input id="file" ref="file" type="file" @change="fileChange" v-if="file === null"/>
                            </el-form-item>
                            <el-form-item :label="$t('sendMessage.fee')">
                                <el-button class="calculate_fee" @click="getMessageFee()">{{$t('sendMessage.calculate')}}</el-button>
                                <input class="el-input__inner"  v-model="messageForm.fee" type="number" min="1" max="100000" :step="0.1"/>
                                <label class="input_suffix">SS</label>
                            </el-form-item>
                            <el-form-item :label="$t('sendMessage.secret_key')">
                                <el-input v-model="messageForm.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="sendMessageInfo">{{$t('sendMessage.send_message')}}</button>
                    </div>
                </div>
            </div>
        </div>
        <!--view transfer account dialog-->
        <div class="modal" id="transfer_accounts_modal" v-show="tranferAccountsDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">{{$t('transfer.transfer_title')}}</h4>
                    </div>
                    <div class="modal-body modal-message">
                        <el-form>
                            <el-form-item :label="$t('transfer.receiver')" class="item_receiver">
                                <masked-input id="tranfer_receiver" mask="AAA-****-****-****-*****" v-model="transfer.receiver"/>
                                <img src="../../assets/img/account_directory.svg"/>
                            </el-form-item>
                            <el-form-item :label="$t('transfer.receiver_public_key')" v-if="transfer.hasPublicKey">
                                <el-input v-model="transfer.receiverPublickey" type="password"></el-input>
                            </el-form-item>
                            <el-form-item :label="$t('transfer.amount')">
                                <input class="el-input__inner"  v-model="transfer.number" min="0" :max="1000000000" type="number"/>
                                <label class="input_suffix">SS</label>
                            </el-form-item>
                            <el-form-item :label="$t('transfer.fee')">
                                <el-button class="calculate_fee" @click="getTransferFee()">{{$t('transfer.calculate')}}</el-button>
                                <input class="el-input__inner"  v-model="transfer.fee"  min="1" max="100000" :step="0.1" type="number"/>
                                <label class="input_suffix">SS</label>
                            </el-form-item>
                            <el-form-item label="">
                                <el-checkbox v-model="transfer.hasMessage">{{$t('transfer.enable_add_info')}}</el-checkbox>
                                <el-checkbox ref="encrypted2" v-model="transfer.isEncrypted"
                                             :disabled="!transfer.hasMessage">{{$t('transfer.encrypted_information')}}
                                </el-checkbox>
                                <el-input
                                    type="textarea"
                                    :autosize="{ minRows: 2, maxRows: 10}"
                                    resize="none"
                                    :placeholder="$t('transfer.message_tip')"
                                    v-model="transfer.message"
                                    :disabled="!transfer.hasMessage">
                                </el-input>
                            </el-form-item>
                            <el-form-item :label="$t('transfer.secret_key')">
                                <el-input v-model="transfer.password" type="password"></el-input>
                            </el-form-item>
                        </el-form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="sendTransferInfo">{{$t('transfer.transfer_send')}}</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal_hubSetting" id="hub_init_setting" v-show="hubInitDialog">
            <div class="modal-header">
                <h4 class="modal-title">
                    <span>{{$t('login.init_hub')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <el-form label-position="left" :label-width="this.$i18n.locale === 'en'? '200px':'160px'">
                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account')">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account_password')">
                        <el-input type="password" v-model="hubsetting.sharderPwd" @blur="checkSharder"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_address')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_port')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_clent_privateKey')"  v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.public_ip_address')">
                        <el-input v-model="hubsetting.publicAddress" :disabled="hubsetting.openPunchthrough"></el-input>
                    </el-form-item>
                    <el-form-item class="create_account" :label="$t('hubsetting.token_address')">
                        <el-input  v-model="hubsetting.SS_Address"></el-input>
                        <!--<a @click="register"><span>创建账户</span></a>-->
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.enable_auto_mining')">
                        <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.set_mnemonic_phrase')" v-if="hubsetting.isOpenMining">
                        <el-input type="password" v-model="hubsetting.modifyMnemonicWord"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.set_password')">
                        <el-input type="password" v-model="hubsetting.newPwd"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.confirm_password')">
                        <el-input type="password" v-model="hubsetting.confirmPwd"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn" @click="verifyHubSetting">{{$t('hubsetting.confirm_restart')}}</button>
                    <button class="common_btn" @click="closeDialog">{{$t('hubsetting.cancel')}}</button>
                </div>
            </div>
        </div>
        <!--view tranfer account dialog-->
        <div class="modal_hubSetting" id="hub_setting" v-show="hubSettingDialog">
            <div class="modal-header">
                <button class="common_btn" @click="openAdminDialog('reset')">{{$t('hubsetting.reset')}}</button>
                <button class="common_btn" @click="openAdminDialog('restart')">{{$t('hubsetting.restart')}}</button>
                <h4 class="modal-title">
                    <span>{{$t('hubsetting.title')}}</span>
                </h4>

            </div>
            <div class="modal-body">
                <div class="version_info">
                    <span>{{$t('hubsetting.current_version')}}</span>
                    <span>{{blockchainState.version}}</span>
                    <span v-if="isUpdate">{{$t('hubsetting.discover_new_version')}}{{latesetVersion}}</span>
                    <span v-if="isUpdate" @click="openAdminDialog('update')">{{$t('hubsetting.update')}}</span>
                </div>
                <el-form label-position="left" label-width="160px">
                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account')">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account_password')">
                        <el-input v-model="hubsetting.sharderPwd" @blur="checkSharder"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_address')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_port')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_clent_privateKey')"  v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.public_ip_address')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.publicAddress" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.token_address')">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.enable_auto_mining')">
                        <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.reset_mnemonic_phrase')" v-if="hubsetting.isOpenMining">
                        <el-input v-model="hubsetting.modifyMnemonicWord"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.reset_password')">
                        <el-input v-model="hubsetting.newPwd"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.confirm_password')">
                        <el-input v-model="hubsetting.confirmPwd"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn" @click="openAdminDialog('reConfig')">{{$t('hubsetting.confirm_restart')}}</button>
                    <button class="common_btn" @click="closeDialog()">{{$t('hubsetting.cancel')}}</button>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="account_info" v-show="userInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('account_info.account_information')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                    <tr>
                        <th>{{$t('account_info.accountID')}}</th>
                        <td>{{accountInfo.account}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.account_address')}}</th>
                        <td>{{accountInfo.accountRS}}</td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.account_name')}}</th>
                        <td>
                            <div class="accountName" v-if="isShowName">
                                <span v-if="typeof accountInfo.name !== 'undefined' && accountInfo.name !== ''">{{accountInfo.name}}</span>
                                <span v-else style="color:#999;font-weight: normal">{{$t('account_info.account_name_not_set')}}</span>
                                <img src="../../assets/img/rewrite.svg" @click="isShowName = false"/>
                            </div>
                            <div class="rewriteName" v-else>
                                <el-input v-model="temporaryName"></el-input>
                                <button class="common_btn" @click="openSecretPhraseDialog">{{$t('account_info.account_set_name')}}</button>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.account_balance')}}</th>
                        <td>{{$global.formatMoney(accountInfo.balanceNQT/100000000)}} SS</td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.account_available_balance')}}</th>
                        <td>{{$global.formatMoney(accountInfo.effectiveBalanceSS)}} SS</td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.account_mining_balance')}}</th>
                        <td>{{$global.formatMoney(accountInfo.forgedBalanceNQT/100000000)}} SS</td>
                    </tr>
                    <tr>
                        <th>{{$t('account_info.public_key')}}</th>
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
        <secretPhrase :openDialog="secretPhraseDialog" @getPwd="getSecretPhrase" @isClose="isClose"></secretPhrase>





    </div>
</template>
<script>
    import echarts from "echarts";
    import dialogCommon from "../dialog/dialog_common";
    import adminPwd from "../dialog/adminPwd";
    import secretPhrase from "../dialog/secretPhrase";

    export default {
        name: "Network",
        components: {echarts,dialogCommon,adminPwd,secretPhrase,
            "masked-input": require("vue-masked-input").default},
        data () {
            return {
                //dialog
                sendMessageDialog: false,
                tranferAccountsDialog: false,
                hubSettingDialog: false,
                hubInitDialog:false,


                tradingInfoDialog: false,
                userInfoDialog:false,
                accountInfoDialog: false,
                adminPasswordDialog:false,
                secretPhraseDialog:false,
                initHUb:this.$store.state.isHubInit,


                isShowName: true,
                generatorRS:'',
                secretPhrase:SSO.secretPhrase,

                blockInfoDialog:false,
                height:'',

                publicKey:SSO.publicKey,
                messageForm: {
                    errorCode:false,
                    receiver: "SSA-____-____-____-_____",
                    message: "",
                    isEncrypted: false,
                    hasPublicKey:false,
                    isFile:false,
                    publicKey:"",
                    senderPublickey:SSO.publicKey,
                    fileName:"",
                    password: "",
                    fee:1
                },
                file:null,
                transfer: {
                    receiver: "SSA-____-____-____-_____",
                    number: 0,
                    fee: 1,
                    hasMessage: false,
                    message: "",
                    isEncrypted: false,
                    password: "",
                    hasPublicKey:false,
                    receiverPublickey:"",
                    errorCode:false
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
                unconfirmedTransactionsList:[],
                blockchainState:this.$global.blockchainState,
                accountInfo:{
                    account:'',
                    accountRS:SSO.accountRS,
                    balanceNQT:0,              //账户余额
                    effectiveBalanceSS:0,      //可用余额
                    forgedBalanceNQT:0,        //挖矿余额
                    frozenBalanceNQT:0,        //冻结余额
                    guaranteedBalanceNQT:0,    //保证余额
                    publicKey: SSO.publicKey,
                    requestProcessingTime: '',
                    unconfirmedBalanceNQT: '',
                },
                selectType:'',
                transactionType:[{
                    value:'',
                    label:this.$t('transaction.transaction_type_all')
                },{
                    value:0,
                    label:this.$t('transaction.transaction_type_payment')
                },{
                    value:1,
                    label:this.$t('transaction.transaction_type_information')
                },{
                    value:1.5,
                    label:this.$t('transaction.transaction_type_account')
                },{
                    value:6,
                    label:this.$t('transaction.transaction_type_storage_service')
                },{
                    value:8,
                    label:this.$t('transaction.transaction_type_forge_pool')
                },{
                    value:9,
                    label:this.$t('transaction.transaction_type_block_reward')
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
                temporaryName:'',
                ssPublickey:SSO.publicKey

            };
        },
        created(){
            const _this = this;

            console.log("_this.initHUb",_this.initHUb);

            _this.getAccount(_this.accountInfo.accountRS).then(res=>{
                _this.accountInfo.account = res.account;
                _this.accountInfo.balanceNQT = res.balanceNQT;
                _this.accountInfo.effectiveBalanceSS = res.effectiveBalanceSS;
                _this.accountInfo.forgedBalanceNQT = res.forgedBalanceNQT;
                _this.accountInfo.frozenBalanceNQT = res.frozenBalanceNQT;
                _this.accountInfo.guaranteedBalanceNQT = res.guaranteedBalanceNQT;
                _this.accountInfo.unconfirmedBalanceNQT = res.unconfirmedBalanceNQT;
            });

            _this.getAccountTransactionList();
            _this.$global.setBlockchainState(_this).then(res=>{
                _this.blockchainState = res;
            });

            SSO.getState();

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

                _this.isUpdate = bool;
            }).catch(err=>{
                console.log(err);
            });
        },
        methods: {
            drawBarchart: function (barchat) {
                const barchart = echarts.init(document.getElementById("transaction_amount_bar"));
                const _this = this;
                const option = {
                    grid: {
                        left: '15%',
                        right: '2%',
                        top: '10%',
                        bottom: '15%',
                    },
                    tooltip: {
                        trigger: 'axis'
                    },
                    xAxis: {
                        type: 'category',
                        data:barchat.xAxis,
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [{
                        data:barchat.series,
                        type: 'bar',
                        color: '#493eda',
                    }]
                };
                if (option && typeof option === "object") {
                    barchart.setOption(option, true);
                }
            },
            drawYield: function (yields) {
                const yieldCurve = echarts.init(document.getElementById("yield_curve"));
                const _this = this;
                const option = {
                    grid: {
                        left: '15%',
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
                        data: yields.xAxis,
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [{
                        data: yields.series,
                        type: 'line',
                        color: '#493eda',
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
                        _this.$message.success(_this.$t('notification.update_success'));
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
                    _this.$message.success(_this.$t('notification.restart_success'));
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
                    _this.$message.success(_this.$t('notification.restart_success'));
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            updateHubSetting(adminPwd, params){
                const _this = this;
                params.append("adminPassword",adminPwd);
                this.$http.post('/sharder?requestType=reConfig', params).then(res => {
                    if(typeof res.data.errorDescription === "undefined"){
                        _this.$message.error(res.data.errorDescription);
                    }else{
                        _this.$message.success(_this.$t('notification.restart_success'));
                    }
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            verifyHubSettingInfo(){
                const _this = this;
                let formData = new FormData();

                if(_this.hubsetting.openPunchthrough){
                    formData.append("sharder.useNATService",true);
                    if(_this.hubsetting.address === '' ||
                        _this.hubsetting.port === '' ||
                        _this.hubsetting.clientSecretkey === ''){
                        if(_this.hubsetting.sharderPwd === '')
                            _this.$message.error(_this.$t('notification.hubsetting_no_sharder_account'));
                        else
                            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
                        return false;
                    }else{
                        formData.append("sharder.NATServiceAddress",_this.hubsetting.address);
                        formData.append("sharder.NATServicePort",_this.hubsetting.port);
                        formData.append("sharder.NATClientKey",_this.hubsetting.clientSecretkey);
                        formData.append("sharder.myAddress", _this.hubsetting.publicAddress);
                    }
                }else{
                    formData.append("sharder.useNATService",false);
                }

                if(_this.hubsetting.SS_Address !== ''){
                    const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                    if(!_this.hubsetting.SS_Address.toUpperCase().match(pattern)){
                        _this.$message.warning(_this.$t('notification.hubsetting_account_address_error_format'));
                        return false;
                    }else{
                        formData.append("sharder.HubBindAddress",_this.hubsetting.SS_Address);
                        formData.append("reBind",true);
                    }
                }else{
                    formData.append("reBind",false);
                }

                if(_this.hubsetting.isOpenMining){
                    formData.append("sharder.HubBind",true);
                    if(_this.hubsetting.modifyMnemonicWord === ''){
                        _this.$message.warning(_this.$t('notification.hubsetting_no_mnemonic_word'));
                        return false;
                    }
                    formData.append("sharder.HubBindPassPhrase",_this.hubsetting.modifyMnemonicWord);
                }else{
                    formData.append("sharder.HubBind",false);
                }
                formData.append("restart",false);
                formData.append("sharder.disableAdminPassword",false);


                if(_this.hubsetting.newPwd !== "" || _this.hubsetting.confirmPwd !== ""){
                    if(_this.hubsetting.newPwd !== _this.hubsetting.confirmPwd){
                        _this.$message.warning(_this.$t('notification.hubsetting_inconsistent_password'));
                        return false;
                    }else{
                        formData.append("newAdminPassword",_this.hubsetting.newPwd);
                    }
                }
                return formData;
            },
            verifyHubSetting:function(){
                const _this = this;
                let formData = _this.verifyHubSettingInfo();
                if(formData === false){
                    return;
                }else{
                    formData.append("isInit",true);
                }
                this.$http.post('/sharder?requestType=reConfig', formData).then(res => {
                    if(typeof res.data.errorDescription === 'undefined'){
                        _this.$message.success(_this.$t('notification.restart_success'));
                        _this.hubSettingDialog = false;
                        this.$store.state.mask = false;
                        this.$router.push("/login");
                    }else{
                        _this.$message.error(res.data.errorDescription);
                    }
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            checkSharder(){
                const _this = this;
                let formData = new FormData();
                if(_this.hubsetting.sharderAccount !== '' && _this.hubsetting.sharderPwd !== '' && _this.hubsetting.openPunchthrough){
                    formData.append("username",_this.hubsetting.sharderAccount);
                    formData.append("password",_this.hubsetting.sharderPwd);
                    _this.$http.post('https://taskhall.sharder.org/bounties/hubDirectory/check.ss',formData).then(res=>{
                        if(res.data.status === 'success'){
                            _this.hubsetting.address = res.data.data.natServiceAddress;
                            _this.hubsetting.port = res.data.data.natServicePort;
                            _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
                            _this.hubsetting.publicAddress = res.data.data.hubAddress;
                            _this.hubsetting.SS_Address = '';
                        }else if(res.data.errorType === 'unifiedUserIsNull'){
                            _this.$message.error(res.data.errorMessage);
                        }else if(res.data.errorType === 'hubDirectoryIsNull'){
                            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
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
                        resolve(res.data);
                        // console.log(_this.accountInfo);
                    }).catch(function (err) {
                        console.log(err);
                    });
                });
            },
            getMessageFee:function(){
                const _this = this;
                let options = {};
                let encrypted = {};
                let formData = new FormData();


                _this.getAccount(SSO.account).then(res=>{
                    if(res.errorDescription === "Unknown account"){
                        _this.$message.warning(_this.$t('notification.new_account_warning'));
                        return;
                    }
                });

                if(_this.messageForm.receiver === "SSA-____-____-____-_____" ||
                    _this.messageForm.receiver === "___-____-____-____-_____"){
                    formData.append("recipient", "");
                }else{
                    formData.append("recipient",  _this.messageForm.receiver);
                    formData.append("recipientPublicKey",  _this.messageForm.publicKey);

                }


                // if(_this.messageForm.errorCode){
                //     _this.$message.warning(_this.$t('notification.null_information_warning'));
                //     return;
                // }
                formData.append("phased", 'false');
                formData.append("phasingLinkedFullHash", '');
                formData.append("phasingHashedSecret", '');
                formData.append("phasingHashedSecretAlgorithm", '2');
                formData.append("calculateFee", 'true');
                formData.append("broadcast", 'false');
                formData.append("feeNQT", '0');
                formData.append("publicKey", SSO.publicKey);
                formData.append("deadline", '1440');

                if(_this.messageForm.isEncrypted){

                    if(_this.messageForm.receiver === "SSA-____-____-____-_____" ||
                        _this.messageForm.receiver === "___-____-____-____-_____"){
                        _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
                        return;
                    }
                    if(_this.messageForm.publicKey === ""){
                        _this.$message.warning(_this.$t('notification.sendmessage_null_account_public'));
                        return;
                    }
                    if(_this.messageForm.password === ""){
                        _this.$message.warning(_this.$t('notification.sendmessage_null_secret_key'));
                        return;
                    }
                    options.account = _this.messageForm.receiver;
                    options.publicKey = _this.messageForm.publicKey;
                    if(_this.messageForm.isFile){
                        formData.append("messageToEncryptIsText",'false');
                        formData.append("encryptedMessageIsPrunable",'true');
                        let encryptionkeys = SSO.getEncryptionKeys(options, _this.messageForm.password);

                        _this.encryptFileCallback(_this.file, encryptionkeys).then(res=>{
                            formData.append("encryptedMessageFile", res.file);
                            formData.append("encryptedMessageNonce", converters.byteArrayToHexString(res.nonce));
                            _this.sendMessage(formData);

                        });
                    }else{
                        encrypted = SSO.encryptNote(_this.messageForm.message, options, _this.messageForm.password);
                        formData.append("encrypt_message",'1');
                        formData.append("encryptedMessageData", encrypted.message);
                        formData.append("encryptedMessageNonce", encrypted.nonce);
                        formData.append("messageToEncryptIsText", 'true');
                        formData.append("encryptedMessageIsPrunable", 'true');
                        _this.sendMessage(formData);

                    }
                }else{
                    if(_this.messageForm.isFile) {
                        console.log(_this.file);
                        formData.append("messageFile",_this.file);
                        formData.append("messageIsText", 'false');
                        formData.append("messageIsPrunable", 'true');
                    }else{
                        if(_this.messageForm.message !== ""){
                            formData.append("messageIsText", 'true');
                            formData.append("message", _this.messageForm.message);
                            if(_this.$global.stringToByte(_this.messageForm.message).length >= 28){    //28 MIN_PRUNABLE_MESSAGE_LENGTH
                                formData.append("messageIsPrunable", 'true');
                            }
                        }
                    }
                    _this.sendMessage(formData);
                }
            },
            getTransferFee:function(){
                const _this = this;
                let options = {};
                let encrypted = {};
                let formData = new FormData();

 /*               if(_this.transfer.errorCode){
                    _this.$message.warning(_this.$t('notification.null_information_warning'));
                    return;
                }*/

                if(_this.transfer.receiver === "SSA-____-____-____-_____" ||
                    _this.transfer.receiver === "___-____-____-____-_____"){
                    _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
                    return;
                }
                const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                if(!_this.transfer.receiver.toUpperCase().match(pattern)){
                    _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                    return;
                }

                if(_this.transfer.number === 0){
                    _this.$message.warning(_this.$t('notification.transfer_amount_error'));
                    return;
                }
                _this.getAccount(_this.accountInfo.accountRS).then(res=>{
                    if(typeof res.errorDescription === 'undefined') {
                        if(res.errorDescription === "Unknown account"){
                            _this.$message.warning(_this.$t('notification.new_account_warning'));
                            return;
                        }
                    }
                    _this.accountInfo = res;
                    if(_this.transfer.number > _this.accountInfo.unconfirmedBalanceNQT/100000000){
                        _this.$message.warning(_this.$t('notification.transfer_balance_insufficient'));
                        return;
                    }
                    if(typeof SSO.secretPhrase === "undefined" && _this.transfer.password === ""){
                        _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
                        return;
                    }
                    formData.append("recipient",_this.transfer.receiver);
                    formData.append("deadline","1440");
                    formData.append("phased", 'false');
                    formData.append("phasingLinkedFullHash", '');
                    formData.append("phasingHashedSecret", '');
                    formData.append("phasingHashedSecretAlgorithm", '2');
                    formData.append("publicKey",res.publicKey);
                    formData.append("calculateFee","true");
                    formData.append("broadcast","false");
                    formData.append("feeNQT","0");
                    formData.append("amountNQT",_this.transfer.number * 100000000);

                    if(_this.transfer.hasMessage && _this.transfer.message !== ""){
                        if(_this.transfer.isEncrypted){
                            if(_this.transfer.password === ""){
                                _this.$message.warning(_this.$t('notification.sendmessage_null_secret_key'));
                                return;
                            }
                            if(_this.transfer.receiverPublickey === ""){
                                _this.$message.warning(_this.$t('notification.transfer_null_public_key'));
                                return;
                            }

                            options.account = _this.transfer.receiver;
                            options.publicKey = _this.transfer.receiverPublickey;

                            encrypted = SSO.encryptNote(_this.transfer.message, options, _this.transfer.password);
                            formData.append("encrypt_message",'1');
                            formData.append("encryptedMessageData", encrypted.message);
                            formData.append("encryptedMessageNonce", encrypted.nonce);
                            formData.append("messageToEncryptIsText", 'true');
                            formData.append("encryptedMessageIsPrunable", 'true');
                            // _this.sendMessage(formData);

                        }else{
                            formData.append("message",_this.transfer.message);
                            formData.append("messageIsText","true");
                        }
                    }


                    _this.sendTransfer(formData);
                });
            },
            encryptFileCallback:function(file,encryptionkeys){
                return new Promise(function (resolve, reject) {
                    SSO.encryptFile(file,encryptionkeys,function (encrypted) {
                        resolve(encrypted);
                    });
                });
            },
            sendMessageInfo:function(){
                const _this = this;
                let options = {};
                let encrypted = {};
                let formData = new FormData();
                console.log(_this.messageForm);
                if(_this.messageForm.receiver === "SSA-____-____-____-_____" ||
                    _this.messageForm.receiver === "___-____-____-____-_____" ||
                    _this.messageForm.receiver === "SSA" ||
                    _this.messageForm.receiver === ""){
                    _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
                    return;
                }
                const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                if(!_this.messageForm.receiver.toUpperCase().match(pattern)){
                    _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                    return;
                }

                if(_this.messageForm.hasPublicKey){
                    if(_this.messageForm.publicKey === ""){
                        _this.$message.warning(_this.$t('notification.transfer_null_public_key'));
                        return;
                    }
                }
/*                if(_this.messageForm.errorCode){
                    _this.$message.warning(_this.$t('notification.null_information_warning'));
                    return;
                }*/
                if(_this.messageForm.password === ''){
                    _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
                    return;
                }

                formData.append("recipient",_this.messageForm.receiver);
                formData.append("recipientPublicKey",_this.messageForm.publicKey);
                formData.append("phased", 'false');
                formData.append("phasingLinkedFullHash", '');
                formData.append("phasingHashedSecret", '');
                formData.append("phasingHashedSecretAlgorithm", '2');
                formData.append("feeNQT", _this.messageForm.fee * 100000000);
                formData.append("secretPhrase", _this.messageForm.password);
                formData.append("deadline", '1440');

                if(!_this.messageForm.isEncrypted){
                    if(_this.file === null){
                        if(_this.messageForm.message !== ""){
                            formData.append("messageIsText", 'true');
                            formData.append("message", _this.messageForm.message);
                            if(_this.$global.stringToByte(_this.messageForm.message).length >= 28){    //28 MIN_PRUNABLE_MESSAGE_LENGTH
                                formData.append("messageIsPrunable", 'true');
                            }
                        }
                    }else{
                        formData.append("messageFile",_this.file);
                        formData.append("messageIsText", 'false');
                        formData.append("messageIsPrunable", 'true');
                    }
                    _this.sendMessage(formData);
                }else{
                    options.account = _this.messageForm.receiver;
                    options.publicKey = _this.messageForm.publicKey;

                    if(_this.file === null){
                        encrypted = SSO.encryptNote(_this.messageForm.message, options, _this.messageForm.password);
                        formData.append("encrypt_message",'1');
                        formData.append("encryptedMessageData", encrypted.message);
                        formData.append("encryptedMessageNonce", encrypted.nonce);
                        formData.append("messageToEncryptIsText", 'true');
                        formData.append("encryptedMessageIsPrunable", 'true');
                        _this.sendMessage(formData);
                    }else{
                        formData.append("messageToEncryptIsText",'false');
                        formData.append("encryptedMessageIsPrunable",'true');
                        let encryptionkeys = SSO.getEncryptionKeys(options, _this.messageForm.password);
                        _this.encryptFileCallback(_this.file, encryptionkeys).then(res=>{
                            formData.append("encryptedMessageFile", res.file);
                            formData.append("encryptedMessageNonce", converters.byteArrayToHexString(res.nonce));
                            _this.sendMessage(formData);

                        });
                    }
                }
            },
            sendMessage:function(formData){
                const _this = this;
                return new Promise(function (resolve, reject) {
                    let config = {
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        }
                    };
                    _this.$http.post('/sharder?requestType=sendMessage',formData, config).then(res=>{

                        if(typeof res.data.errorDescription === 'undefined'){
                            if(res.data.broadcasted){
                                _this.$message.success(_this.$t('notification.sendmessage_success'));
                                resolve(res.data);
                                _this.closeDialog();
                                _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res=>{
                                    _this.$store.commit("setUnconfirmedNotificationsList",res.unconfirmedTransactions);
                                });
                            }else{
                                console.log(res.data);
                                _this.messageForm.fee = res.data.transactionJSON.feeNQT / 100000000;
                                resolve(res.data);
                            }
                        }else{
                            _this.$message.error(res.data.errorDescription);
                            resolve(res.data);
                        }
                    }).catch(err=>{
                        reject(err);
                        console.log(err);
                        _this.$message.error(err);
                    });
                });

            },
            sendTransferInfo:function(){
                const _this = this;
                let options = {};
                let encrypted = {};
                let formData = new FormData();

/*                if(_this.transfer.errorCode){
                    _this.$message.warning(_this.$t('notification.null_information_warning'));
                    return;
                }*/

                if(_this.transfer.receiver === "SSA-____-____-____-_____" ||
                    _this.transfer.receiver === "___-____-____-____-_____" ||
                    _this.transfer.receiver === "SSA" ||
                    _this.transfer.receiver === ""){
                    _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
                    return;
                }
                const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                if(!_this.transfer.receiver.toUpperCase().match(pattern)){
                    _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                    return;
                }
                if(_this.transfer.receiverPublickey ===""){
                    _this.$message.warning(_this.$t('notification.sendmessage_null_account_public'));
                    return;
                }
                if(_this.transfer.number === 0){
                    _this.$message.warning(_this.$t('notification.transfer_amount_error'));
                    return;
                }
                _this.getAccount(_this.accountInfo.accountRS).then(res=>{
                    if(typeof res.errorDescription === 'undefined') {
                        if(res.errorDescription === "Unknown account"){
                            _this.$message.warning(_this.$t('notification.new_account_warning'));
                            return;
                        }
                    }
                    _this.accountInfo = res;
                    if(_this.transfer.number > _this.accountInfo.unconfirmedBalanceNQT/100000000){
                        _this.$message.warning(_this.$t('notification.transfer_balance_insufficient'));
                        return;
                    }

                    if(_this.transfer.password === ""){
                        _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
                        return;
                    }

                    formData.append("recipient",_this.transfer.receiver);
                    formData.append("recipientPublicKey",_this.transfer.receiverPublickey);
                    formData.append("deadline","1440");
                    formData.append("phased", 'false');
                    formData.append("phasingLinkedFullHash", '');
                    formData.append("phasingHashedSecret", '');
                    formData.append("phasingHashedSecretAlgorithm", '2');
                    formData.append("publicKey","");
                    formData.append("feeNQT",_this.transfer.fee * 100000000);
                    formData.append("amountNQT",_this.transfer.number * 100000000);
                    formData.append("secretPhrase",_this.transfer.password);

                    if(_this.transfer.hasMessage && _this.transfer.message !== ""){
                        if(_this.transfer.isEncrypted){

                            options.account = _this.transfer.receiver;
                            options.publicKey = _this.transfer.receiverPublickey;
                            encrypted = SSO.encryptNote(_this.transfer.message, options, _this.transfer.password);
                            formData.append("encrypt_message",'1');
                            formData.append("encryptedMessageData", encrypted.message);
                            formData.append("encryptedMessageNonce", encrypted.nonce);
                            formData.append("messageToEncryptIsText", 'true');
                            formData.append("encryptedMessageIsPrunable", 'true');
                            // _this.sendMessage(formData);

                        }else{
                            formData.append("message",_this.transfer.message);
                            formData.append("messageIsText","true");
                        }
                    }
                    _this.sendTransfer(formData);
                });
            },
            sendTransfer:function(formData){
                const _this = this;
                return new Promise(function (resolve, reject) {
                    let config = {
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        }
                    };
                    _this.$http.post('/sharder?requestType=sendMoney',formData, config).then(res=>{

                        if(typeof res.data.errorDescription === 'undefined'){
                            if(res.data.broadcasted){
                                _this.$message.success(_this.$t('notification.transfer_success'));
                                resolve(res.data);
                                _this.closeDialog();
                                _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res=>{
                                    _this.$store.commit("setUnconfirmedNotificationsList",res.unconfirmedTransactions);
                                });
                            }else{
                                console.log(res.data);
                                _this.transfer.fee = res.data.transactionJSON.feeNQT / 100000000;
                                resolve(res.data);
                            }
                        }else{
                            _this.$message.error(res.data.errorDescription);
                            resolve(res.data);
                        }
                    }).catch(err=>{
                        reject(err);
                        console.log(err);
                        _this.$message.error(err);
                    });
                });
            },
            getAccountTransactionList:function(){
                const _this = this;
                let params = new URLSearchParams();

                params.append("account",_this.accountInfo.accountRS);
                // params.append("firstIndex",0);
                // params.append("firstIndex",0);

                if(_this.selectType === 1.5){
                    params.append("type","1");
                    params.append("subtype","5");
                }else if(_this.selectType === 1){
                    params.append("type","1");
                    params.append("subtype","0");
                }else{
                    params.append("type",_this.selectType);
                }


                this.$http.get('/sharder?requestType=getBlockchainTransactions',{params}).then(function (res) {
                    _this.accountTransactionList =res.data.transactions;
                    // console.log("_this.accountTransactionList",_this.accountTransactionList);
                    _this.totalSize = _this.accountTransactionList.length;
                    _this.unconfirmedTransactionsList = "";
                    _this.getTotalList();
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
            },
            openHubInitDialog:function(){
                const _this = this;
                _this.$store.state.mask = true;
                _this.hubInitDialog = true;
            },
            openTradingInfoDialog:function(trading){
                this.trading = trading;
                this.tradingInfoDialog = true;
            },
            openUserInfoDialog:function(){
                this.userInfoDialog = true;
                this.$store.state.mask = true;
            },
            openAccountInfoDialog: function (account) {
                this.generatorRS = account;
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
            openSecretPhraseDialog:function(){
                const _this = this;
                _this.userInfoDialog = false;
                _this.secretPhraseDialog =true;
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
            getSecretPhrase:function(secretPhrase){
                const _this = this;
                _this.secretPhraseDialog =false;
                _this.setName(secretPhrase);
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.sendMessageDialog = false;
                this.tranferAccountsDialog = false;
                this.hubSettingDialog = false;
                this.hubInitDialog = false;
                this.tradingInfoDialog = false;
                this.accountInfoDialog = false;
                this.userInfoDialog = false;

                const _this = this;
                _this.messageForm.errorCode = false;
                _this.messageForm.receiver =  "SSA";
                _this.messageForm.message =  "";
                _this.messageForm.isEncrypted =  false;
                _this.messageForm.hasPublicKey = false;
                _this.messageForm.isFile = false;
                _this.messageForm.publicKey = "";
                _this.messageForm.senderPublickey = SSO.publicKey;
                _this.messageForm.fileName = "";
                _this.messageForm.password = "";
                _this.messageForm.fee = 1;
                _this.file = null;

                _this.transfer.receiver = "SSA";
                _this.transfer.number = 0;
                _this.transfer.fee = 1;
                _this.transfer.hasMessage = false;
                _this.transfer.message = "";
                _this.transfer.isEncrypted = false;
                _this.transfer.password = "";
                _this.transfer.hasPublicKey = false;
                _this.transfer.receiverPublickey = "";
                _this.transfer.errorCode = false;



                _this.isShowName = true;
                _this.temporaryName = "";

            },
            copySuccess: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: _this.$t('notification.clipboard_success'),
                    type: "success"
                });
            },
            setName:function(secretPhrase){
                const _this = this;
                let formData = new FormData();
                console.log("dingwei");
                formData.append("name",_this.temporaryName);
                formData.append("secretPhrase",secretPhrase);
                formData.append("deadline","1440");
                formData.append("phased","false");
                formData.append("phasingLinkedFullHash","");
                formData.append("phasingHashedSecret","");
                formData.append("phasingHashedSecretAlgorithm","2");
                formData.append("feeNQT","0");

                _this.$http.post('/sharder?requestType=setAccountInfo',formData).then(res=>{
                    if(typeof res.data.errorDescription === "undefined"){
                        _this.$message.success(_this.$t('notification.modify_success'));
                        _this.accountInfo.name = res.data.transactionJSON.attachment.name;
                        _this.isShowName = true;
                        _this.temporaryName = "";
                    }else{
                        _this.$message.error(res.data.errorDescription);
                        _this.accountInfo.name = "";
                        _this.isShowName = true;
                    }
                })

            },
            copyError: function () {
                const _this = this;
                _this.$message({
                    showClose: true,
                    message: _this.$t('notification.clipboard_error'),
                    type: "error"

                });
            },
            delFile:function(){
                const _this = this;
                $('#file').val("");
                _this.messageForm.fileName = "";
                _this.file = null;
                _this.messageForm.isFile = false;
            },
            fileChange: function (e) {
                const _this = this;
                _this.messageForm.fileName = e.target.files[0].name;
                _this.file = document.getElementById("file").files[0];

                if(_this.file.size > 1024*1024*5){
                    _this.delFile();
                    _this.$message.error(_this.$t('notification.file_exceeds_max_limit'));
                    return;
                }
                _this.messageForm.isFile = true;
                _this.messageForm.message = "";
            },
            isClose:function () {
                const _this = this;
                _this.tradingInfoDialog = false;
                _this.accountInfoDialog = false;
                _this.blockInfoDialog = false;
                _this.adminPasswordDialog = false;
                _this.secretPhraseDialog = false;

                _this.isShowName = true;
                _this.temporaryName = '';

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
            },
            getDrawData(lists){
                const _this = this;
                let j=0;
                let k=0;
                let barchat = {
                    xAxis:[],
                    series:[]
                };
                let yields = {
                    xAxis:[],
                    series:[]
                };
                lists.forEach(function(value,index,array){
                    if(j>=5||k>=7){
                        return;
                    }
                    if(value.type === 9 || value.type === 0){
                        if(value.type === 0 && j<5){
                            j++;
                            if(value.senderRS === SSO.accountRS){
                                barchat.xAxis.push(_this.$t('account.payout'));
                            }else{
                                barchat.xAxis.push(_this.$t('account.income'));
                            }
                            barchat.series.push(value.amountNQT/100000000);
                        }
                        if(k<7 && value.senderRS !== SSO.accountRS){
                            k++;
                            yields.xAxis.push(_this.$global.myFormatTime(value.timestamp, "YMD"));
                            yields.series.push(value.amountNQT/100000000);
                        }
                    }

                });

                for(;j !== 5;j++){
                    barchat.xAxis.push("");
                    barchat.series.push(0);
                }
                for(;k !== 7;k++){
                    yields.xAxis.push("");
                    yields.series.push(0);
                }
                this.drawBarchart(barchat);
                this.drawYield(yields);
            },
            getTotalList:function () {
                const _this = this;
                if(_this.unconfirmedTransactionsList !== _this.$store.state.unconfirmedTransactionsList.unconfirmedTransactions){
                    _this.unconfirmedTransactionsList = _this.$store.state.unconfirmedTransactionsList.unconfirmedTransactions;

                    _this.totalSize = _this.accountTransactionList.length;

                    let list = [];
                    for(let i = 0;i<_this.unconfirmedTransactionsList.length;i++){
                        if(_this.selectType === ''){
                            list.push(_this.unconfirmedTransactionsList[i]);
                            _this.totalSize++;
                        }else{
                            if(_this.selectType === 1 && _this.unconfirmedTransactionsList[i].subtype === 0){
                                list.push(_this.unconfirmedTransactionsList[i]);
                                _this.totalSize++;
                            }else if(_this.selectType !== 1 && _this.selectType === _this.unconfirmedTransactionsList[i].type){
                                list.push(_this.unconfirmedTransactionsList[i]);
                                _this.totalSize++;
                            }else if(_this.selectType === 1.5 &&
                                _this.unconfirmedTransactionsList[i].type === 1 &&
                                _this.unconfirmedTransactionsList[i].subtype === 5){
                                list.push(_this.unconfirmedTransactionsList[i]);
                                _this.totalSize++;
                            }
                        }
                    }

                    for(let i = 0;i<_this.accountTransactionList.length;i++){
                        list.push(_this.accountTransactionList[i]);
                    }
                    console.log("accountTransactionList",list);
                    _this.accountTransactionList = list;

                    if(_this.selectType === '') {
                        _this.getDrawData(_this.accountTransactionList);
                    }
                }
            }
        },
        computed:{
            getLang:function(){
                return this.$store.state.currentLang;
            }
        },
        watch: {
            transfer: {
                handler:function(oldValue,newValue){
                    const _this = this;
                    if(!_this.transfer.hasMessage){
                        _this.transfer.message = "";
                        _this.transfer.isEncrypted = false;
                    }

                    const pattern = /(^[1-9]\d{0,4}$)|(^[1-9]\d{0,4}\.\d$)|(^100000$)/;

                    if(_this.transfer.fee === ''){
                        _this.transfer.fee = 1;
                    }else if(!_this.transfer.fee.toString().match(pattern)){
                        _this.transfer.fee = 1;
                    }

                    const pattern2 = /(^[1-9]\d{0,8}$)|(^1000000000$)|(^0$)/;


                    if(_this.transfer.number === ''){
                        _this.transfer.number = 1;
                    }else if(!_this.transfer.number.toString().match(pattern2)){
                        _this.transfer.number = 1;
                    }

                },
                deep: true
            },
            // hubsetting: {
            //     handler:function(oldValue,newValue){
            //         const _this = this;
            //         if (_this.hubsetting.openPunchthrough) {
            //             _this.checkSharder();
            //         }
            //     },
            //     deep: true
            // },
            messageForm:{
                handler:function(oldValue,newValue){
                    const _this = this;
                    const pattern = /(^[1-9]\d{0,4}$)|(^[1-9]\d{0,4}\.\d$)|(^100000$)/;


                    if(_this.messageForm.fee === ''){
                        _this.messageForm.fee = 1;
                    }else if(!_this.messageForm.fee.toString().match(pattern)){
                        _this.messageForm.fee = 1;
                    }
                },
                deep:true
            },
            selectType:function () {
                const _this = this;

                _this.getAccountTransactionList();
            },
            getLang:{
                handler:function(oldValue,newValue){
                    const _this = this;
                    _this.transactionType = [{
                        value:'',
                        label:this.$t('transaction.transaction_type_all')
                    },{
                        value:0,
                        label:this.$t('transaction.transaction_type_payment')
                    },{
                        value:1,
                        label:this.$t('transaction.transaction_type_information')
                    },{
                        value:1.5,
                        label:this.$t('transaction.transaction_type_account')
                    },{
                        value:6,
                        label:this.$t('transaction.transaction_type_storage_service')
                    },{
                        value:8,
                        label:this.$t('transaction.transaction_type_forge_pool')
                    },{
                        value:9,
                        label:this.$t('transaction.transaction_type_block_reward')
                    }]
                },
                deep:true
            }
        },
        mounted() {
            const _this = this;

            $('#receiver').on("blur",function() {
                let receiver = _this.messageForm.receiver;
                if(receiver !== "___-____-____-____-_____" && receiver !== "SSA-____-____-____-_____"){
                    const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                    if(!receiver.toUpperCase().match(pattern)){
                        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                        return;
                    }
                    if(receiver === _this.accountInfo.accountRS){
                        _this.$message.warning(_this.$t('notification.account_is_self'));
                        _this.messageForm.errorCode = true;
                    }

                    _this.getAccount(receiver).then(res=>{
                        console.log(res);
                        if(res.errorDescription === "Unknown account" && _this.messageForm.publicKey === "") {
                            _this.messageForm.hasPublicKey = true;
                            _this.messageForm.errorCode = true;
                            _this.$message.warning(_this.$t('notification.unknown_account'));
                        }else if(res.errorDescription === "Unknown account" && _this.messageForm.publicKey !== ""){
                            _this.messageForm.hasPublicKey = true;
                            _this.messageForm.errorCode = false;
                        }else if(res.errorDescription === "Incorrect \"account\""){
                            _this.messageForm.errorCode = true;
                            _this.messageForm.hasPublicKey = false;
                            _this.messageForm.publicKey = "";
                            _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                        }else if(typeof res.errorDescription === "undefined"){
                            _this.messageForm.errorCode = false;
                            _this.messageForm.hasPublicKey = false;
                            _this.messageForm.publicKey = res.publicKey;

                        }
                    });
                }
            });
            $('#tranfer_receiver').on("blur",function () {
                let receiver = _this.transfer.receiver;
                if(receiver !== "___-____-____-____-_____" && receiver !== "SSA-____-____-____-_____"){
                    const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                    if(!receiver.toUpperCase().match(pattern)){
                        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                        return;
                    }
                    if(receiver === _this.accountInfo.accountRS){
                        _this.$message.warning(_this.$t('notification.unknown_account'));
                        _this.transfer.errorCode = true;
                    }

                    _this.getAccount(receiver).then(res=>{
                        console.log(res);
                        if(res.errorDescription === "Unknown account" && _this.transfer.receiverPublickey === "") {
                            _this.transfer.hasPublicKey = true;
                            _this.transfer.errorCode = true;
                        }else if(res.errorDescription === "Unknown account" && _this.transfer.receiverPublickey !== ""){
                            _this.transfer.hasPublicKey = true;
                            _this.transfer.errorCode = false;
                            _this.transfer.publicKey = "";
                        }else if(res.errorDescription === "Incorrect \"account\""){
                            _this.transfer.errorCode = true;
                            _this.transfer.hasPublicKey = false;
                            _this.transfer.publicKey = "";
                            _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
                        }else if(typeof res.errorDescription === "undefined"){
                            _this.transfer.errorCode = false;
                            _this.transfer.hasPublicKey = false;
                            _this.transfer.receiverPublickey = res.publicKey;

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

    .calculate_fee{
        background-color: #493eda;
        color: #fff;
        border-radius: 4px;
        border: none;
        width: 35px;
        padding: 0;
        font-size: 13px;
        height: 20px;
    }


    .modal_hubSetting{
        width: 800px!important;
    }
    .modal_hubSetting .modal-header .modal-title{
        margin: 0!important;
    }
    .modal_hubSetting .modal-body{
        padding: 20px 40px 60px!important;
    }
    .modal_hubSetting .modal-body .el-form{
        margin-top: 20px!important;
    }
    /*.modal_hubSetting .modal-body .el-form .create_account .el-input{
        width:450px;
    }*/
    .modal_hubSetting .modal-body .el-form .create_account a{
        position: absolute;
        right: 20px;
        top: 0;
        cursor: pointer;
    }
</style>
