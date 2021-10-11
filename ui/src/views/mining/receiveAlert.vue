<template>
    <div class="receive-alert" v-show="visible && isVisible">
        <div class="active">
            <h1>{{alertInfo.title}}</h1>
            <p>{{alertInfo.content}}</p>
            <button @click="close()">{{$t('mining.attribute.close')}}</button>
        </div>
    </div>
</template>

<script>
    export default {
        name: "receiveAlert",
        props: {
            visible: Boolean,
            alertInfo: {},
        },
        data() {
            return {
                isVisible: true,
            }
        },
        methods: {
            close() {
                this.isVisible = false;
                this.$emit('callback');
            }
        }

    }
</script>

<style scoped type="text/scss" lang="scss">
@import '../../styles/css/vars.scss';
    .receive-alert:after {
        content: "";
        display: inline-block;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: #000;
        opacity: 0.6;
        z-index: 999;
    }

    .receive-alert .active {
        text-align: center;
        position: fixed;
        top: calc(50% - 75px);
        border-radius: 4px;
        background: #fff;
        width: calc(100% - 30px);
        left: 15px;
        z-index: 1000;
        font-size: 14px;
    }

    .receive-alert .active h1 {
        padding: 15px 0 0;
        color: #555;
        font-weight: bold;
    }

    .receive-alert .active p {
        padding: 30px 0;
    }

    .receive-alert .active button {
        border: none;
        outline: none;
        width: 100%;
        height: 40px;
        background: $primary_color;
        color: #fff;
        font-size: 15px;
        border-bottom-left-radius: 4px;
        border-bottom-right-radius: 4px;
    }
</style>
