<template>
    <div class="nodeTypeContent">
        <div class="template">
            <el-row :gutter="20">
                <el-col :xl="6" :lg="6" :md="6" :sm="24" :xs="24">
                    <el-card shadow="hover" class="content">
                        <p> 
                            <strong>IP: </strong>{{ pocInfo.ip }}
                        </p>
                        <p class="node-type"> 
                            <strong>{{ $t('poc.nodeType') }}: </strong>{{ parseNodeType(pocInfo.type) }}
                        </p>
                       <!-- <p class="linked-account">
                            <strong>{{ $t('poc.linkedAccount') }}: </strong><br/>
                            {{ rsAccount(pocInfo.accountId) }}
                        </p>-->
                    </el-card>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<script>
    export default {
        name: "nodeTypeContent",
        props: {
            pocInfo: {}
        },
        methods: {
            parseNodeType(type) {
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
            rsAccount(accountId) {
                //TODO  calculate the right address when the account id < 0 
                let nxtAddress = new NxtAddress();
                let accountRS = "";

                if (nxtAddress.set(accountId)) {
                    accountRS = nxtAddress.toString();
                    console.info("node-accountRS:"+accountRS);
                    return accountRS;
                }
                return accountId;
            }
        }
    }
</script>

<style scoped>
    .nodeTypeContent {
        padding: 10px;
    }
    
    .nodeTypeContent .template p {
        margin-bottom: 5px;
    }

    .nodeTypeContent .template p strong {
        width: 200px;
        font-size: 14px;
        font-weight: bold;
        color: #000;
    }

    .nodeTypeContent .template p span {
        display: inline-block;
        width: 116px;
    }

    .nodeTypeContent .template .content {
        box-shadow: 1px 1px 10px #493eda;
        border-radius: 4px;
    }
</style>
