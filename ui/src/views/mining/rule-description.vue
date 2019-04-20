<template>
    <div class="rule-description">
        <p @click="$router.back()" class="mining-back">{{$t('mining.attribute.return_previous')}}</p>
        <div class="description">
            <h1 class="description-title">{{$t('mining.rule_description.rule_title')}}</h1>
            <p class="description-info"></p>
            <!--<p class="description-info">{{$t('mining.rule_description.rule_subtitle')}}</p>-->
            <div class="info">
                <h3>{{$t('mining.rule_description.sharder_pool')}}</h3>
                <p>
                    {{$t('mining.rule_description.sharder_pool_tip1')}}
                    {{$t('mining.rule_description.sharder_pool_tip2')}}.
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.ss')}}</h3>
                <p>{{$t('mining.rule_description.ss_tip1')}}
                    {{$t('mining.rule_description.ss_tip2')}}
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.poc')}}</h3>
                <p>
                    {{$t('mining.rule_description.poc_tip1')}}
                    {{$t('mining.rule_description.poc_tip2')}}
                </p>
            </div>
            <div class="info">
                <h3>{{$t('mining.rule_description.reward_distribution')}}</h3>
                <p>
                    {{$t('mining.rule_description.reward_distribution_tip1')}}
                    {{$t('mining.rule_description.reward_distribution_tip2')}}
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "rule-description"
    }
</script>

<style scoped>
    .rule-description .description {
        padding: 15px;
        background: #fff;
    }

    .description .description-title {
        font-size: 27px;
        font-weight: bold;
        padding: 30px 0 0 0;
        color: #513ac8;
    }

    .description .description-info {
        color: #513ac8;
        padding: 10px 0 30px
    }

    .rule-description .description .info {
        padding: 15px;
        color: #fff;
        background: #513ac8;
        border-radius: 4px;
        margin: 0 0 15px 0;
    }

    .description .info h3 {
        font-size: 17px;
        font-weight: bold;
        padding: 0 0 10px 0;
    }

</style>
