<template style="width: 100%">
    <div class="messageTxDetail">
        <div class="template">
            <el-row :gutter="20" >
                <el-col :xl="12" :lg="12" :md="12" :sm="24" :xs="24" >
                    <el-card shadow="hover" class="content">

                        <p v-if="rowData.recipientRS === rowData.accountRS || rowData.accountRS === rowData.senderRS" class="node-type">
                            <span v-if="rowData.messageInfo.encryptedMessage">
                                <strong>{{$t('transaction.message_data')}} : </strong>{{formatMessageData(rowData)}}<br>
                                <strong>{{$t('transaction.is_compressed')}} : </strong>{{ rowData.messageInfo.encryptedMessage.isCompressed }}<br>
                                <strong>{{$t('transaction.is_text')}} : </strong>{{ rowData.messageInfo.encryptedMessage.isText }}
                            </span>
                            <span v-if="rowData.messageInfo.message">
                                <strong>{{$t('transaction.message_data')}}:</strong>{{ rowData.messageInfo.message}}<br>
                                <strong>{{$t('transaction.is_text')}}:</strong>{{ rowData.messageInfo.messageIsText}}
                            </span>
                        </p>
                        <p v-if="rowData.messageInfo.encryptedMessage" class="node-type">
                            <strong>{{$t('transaction.message_data')}} : </strong>{{$t('transaction.encrypted_message')}}<br>
                            <strong>{{$t('transaction.is_compressed')}} : </strong>{{ rowData.messageInfo.encryptedMessage.isCompressed }}<br>
                            <strong>{{$t('transaction.is_text')}} : </strong>{{ rowData.messageInfo.encryptedMessage.isText }}
                        </p>

                    </el-card>
                </el-col>
            </el-row>
        </div>

    </div>
</template>

<script>

    export default {
        name: "MessageTxDetail",
        props: {
            rowData: {}
        },
        data(){
            return{
                messageData:'',
                secretPhrase:SSO.secretPhrase,
            }
        },

        methods:{
            formatMessageData(message){
                let options={};

                let otherUser = (message.senderRS == SSO.accountRS ? message.recipientRS : message.senderRS);
                options.nonce = message.messageInfo.encryptedMessage.nonce;
                options.account = otherUser;
                options.isText = message.messageInfo.encryptedMessage.isText;
                options.isCompressed = message.messageInfo.encryptedMessage.isCompressed;
                let decoded = SSO.decryptNote(message.messageInfo.encryptedMessage.data, options, SSO.secretPhrase);
                return decoded.message;

            },
        },
    }

</script>


<style >

    .messageTxDetail {
        padding: 10px;

    }

    .messageTxDetail .template p {
        margin-bottom: 5px;
    }

    .messageTxDetail .template p strong {
        width: 400px;
        font-size: 14px;
        font-weight: bold;
        color: #000;
    }

    .messageTxDetail .template p span {
        display: inline-block;
    }

    .messageTxDetail .template .content {
        box-shadow: 1px 1px 10px #493eda;
        border-radius: 4px;
    }
</style>

