<template>
    <div>
        <NodeTypeContent v-if="rowData.subType === 0" :pocInfo="rowData.pocInfo"></NodeTypeContent>
        <NodeTypeContent v-if="rowData.subType === 1" :pocInfo="rowData.pocInfo"></NodeTypeContent>
        <PocContent v-if="rowData.subType === 2" :pocInfo="rowData.pocInfo"></PocContent>
        <div v-if="rowData.subType === 4" class="generationMissing">
            <p>{{ $t('poc.block_missing') }}</p>
            <p>{{ $t('poc.timeStamp') }} : {{ rowData.pocInfo.missingTimeStamp }}</p>
            <el-tag v-for="(missingAccountId,index) in rowData.pocInfo.missingAccountIds" :key="index">
                {{ missingAccountId }}
            </el-tag>
        </div>
    </div>
</template>

<script>
    import NodeConfigContent from "./nodeConfigContent";
    export default {
        name: "pocDetailContent",
        components: {NodeConfigContent},
        props: {
            rowData: {}
        }
    }
</script>
