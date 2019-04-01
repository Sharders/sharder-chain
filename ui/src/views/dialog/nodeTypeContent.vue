<template>
    <div class="nodeTypeContent">
        <div class="template">
            <el-row :gutter="20">
                <el-col :xl="6" :lg="6" :md="6" :sm="24" :xs="24">
                    <el-card shadow="hover">
                        <p> <strong>IP : </strong>{{ pocInfo.ip }}</p>
                        <p><strong>{{ $t('poc.nodeType') }} : </strong>{{ parseNodeType(pocInfo.type) }}</p>
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
            }
        }
    }
</script>
