<template>
    <div class="modal" id="set_admin_password_modal" v-show="secretPhraseDialog">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button class="close" @click="closeDialog">X</button>
                    <h4 class="modal-title">输入私钥</h4>
                </div>
                <div class="modal-body modal-peer">
                    <p>请输入私钥：</p>
                    <input v-model="secretPhrase" type="password"/>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn" @click="returnValue()">开启</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "secretPhrase",
        props:{
            openDialog:Boolean,
        },
        data(){
            return{
                secretPhraseDialog:false,
                secretPhrase:'',
            }
        },
        methods:{
            returnValue(){
                const _this = this;
                _this.$store.state.mask = false;
                _this.$emit('getPwd',_this.secretPhrase);
            },
            closeDialog(){
                const _this = this;
                _this.$store.state.mask = false;
                _this.$emit('isClose',false);
            }
        },
        watch:{
            openDialog: function (val) {
                const _this = this;
                console.log(val);
                _this.secretPhraseDialog = val;
                if(val)
                {
                    _this.$store.state.mask = true;
                }
            }
        }

    }

</script>

<style scoped>

</style>
