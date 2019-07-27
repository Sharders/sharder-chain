<template style="width: 100%">
    <div class="messageTxDetail">
        <div class="template">
            <el-row :gutter="20" >
                <el-col :xl="6" :lg="6" :md="6" :sm="24" :xs="24" >
                    <el-card shadow="hover" class="content">

                        <p v-if="rowData.messageInfo.encryptedMessage" class="node-type">
                            <strong>{{$t('transaction.encrypted_message_data')}}:</strong>{{ rowData.messageInfo.encryptedMessage.data }}<br>
                            <strong>{{$t('transaction.is_compressed')}}:</strong>{{ rowData.messageInfo.encryptedMessage.isCompressed }}<br>
                            <strong>{{$t('transaction.is_text')}}:</strong>{{ rowData.messageInfo.encryptedMessage.isText }}
                        </p>
                        <p v-if="rowData.messageInfo.encryptedMessageHash" class="node-type">
                            <strong>{{$t('transaction.encrypted_message_hash')}}:</strong>{{rowData.messageInfo.encryptedMessageHash }}
                        </p>
                        <p v-if="rowData.messageInfo.recipientPublicKey" class="node-type">
                            <strong>{{$t('transaction.recipient_public_key')}}:</strong>{{rowData.messageInfo.recipientPublicKey}}
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

