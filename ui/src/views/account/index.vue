<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
  <div>
    <div>
      <el-row v-if="nonePublicKeyHint" class="notice-container">
        <el-col :span="24">
          <div class="notice" style="background: #ffffff">
            <div>
              <a>{{ $t("account.account_inactive") }}</a>
            </div>
          </div>
        </el-col>
      </el-row>
      <div class="block_account mb20">
        <p class="block_title">
          <img src="../../assets/img/account.svg" v-if="projectName === 'mw'" />
          <img
            src="../../assets/img/sharder/account.svg"
            v-else-if="projectName === 'sharder'"
          />
          <span>{{ $t("account.account_title") }}</span>
        </p>
        <div class="w pt60">
          <div class="account_address">
            <span>{{ accountInfo.accountRS }}</span>
            <img
              class="csp pc-i"
              src="../../assets/img/copy.svg"
              v-clipboard:copy="accountInfo.accountRS"
              v-clipboard:success="copySuccess"
              v-clipboard:error="copyError"
              v-if="projectName === 'mw'"
            />
            <img
              class="csp pc-i"
              src="../../assets/img/sharder/copy.svg"
              v-clipboard:copy="accountInfo.accountRS"
              v-clipboard:success="copySuccess"
              v-clipboard:error="copyError"
              v-else-if="projectName === 'sharder'"
            />
          </div>
          <p class="account_info" @click="isUserInfoDialog(true)">
            {{ $t("account.account_info") }}
          </p>
          <p class="account_asset" v-loading="loading">
            {{
              $t("account.assets") +
              $global.formatNQTMoney(accountInfo.effectiveBalanceNQT, 2)
            }}
          </p>
          <div class="account_tool">
            <button
              class="common_btn imgBtn"
              v-bind:class="{
                disabledWriteBtn: !nonDownloading,
                writeBtn: nonDownloading,
              }"
              @click="openTransferDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 174.62 174.83"
                >
                  <path
                    d="M25.2,105.9a75.08,75.08,0,0,1,138-47.3L80.5,141.3a6,6,0,0,0-1.3,6.7,6.28,6.28,0,0,0,5.7,3.7l34.6-.7a6.12,6.12,0,0,0,6-6.2,6,6,0,0,0-1.9-4.4,6.21,6.21,0,0,0-4.3-1.6l-19.5.4,75.4-75.4a6.2,6.2,0,0,0,1-7.3A87,87,0,0,0,33,43.2,86.52,86.52,0,0,0,13.1,107a91.17,91.17,0,0,0,3.3,17.3,5.7,5.7,0,0,0,3,3.6,6.15,6.15,0,0,0,4.6.5,5.86,5.86,0,0,0,3.6-2.9,6.15,6.15,0,0,0,.5-4.6A76.49,76.49,0,0,1,25.2,105.9ZM187,91.3a6.13,6.13,0,0,0-6.6-5.5,6.06,6.06,0,0,0-5.5,6.6A75.26,75.26,0,0,1,106.8,174,76.18,76.18,0,0,1,43,148.1L126.1,65a6.11,6.11,0,0,0,1.2-6.9,6.2,6.2,0,0,0-6.1-3.5L86.4,57.7a6.12,6.12,0,0,0,1.1,12.2l18.1-1.6L30.4,143.4a6.09,6.09,0,0,0-.5,8,89.05,89.05,0,0,0,70.4,35.1c2.5,0,5.1-.1,7.6-0.3A87.56,87.56,0,0,0,187,91.3Z"
                    transform="translate(-12.73 -11.67)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("account.transfer") }}</span>
            </button>
            <button
              class="common_btn imgBtn"
              v-bind:class="{
                disabledWriteBtn: !nonDownloading,
                writeBtn: nonDownloading,
              }"
              @click="openBatchTransferDialog"
              v-if="openAirdrop"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 174.62 174.83"
                >
                  <path
                    d="M25.2,105.9a75.08,75.08,0,0,1,138-47.3L80.5,141.3a6,6,0,0,0-1.3,6.7,6.28,6.28,0,0,0,5.7,3.7l34.6-.7a6.12,6.12,0,0,0,6-6.2,6,6,0,0,0-1.9-4.4,6.21,6.21,0,0,0-4.3-1.6l-19.5.4,75.4-75.4a6.2,6.2,0,0,0,1-7.3A87,87,0,0,0,33,43.2,86.52,86.52,0,0,0,13.1,107a91.17,91.17,0,0,0,3.3,17.3,5.7,5.7,0,0,0,3,3.6,6.15,6.15,0,0,0,4.6.5,5.86,5.86,0,0,0,3.6-2.9,6.15,6.15,0,0,0,.5-4.6A76.49,76.49,0,0,1,25.2,105.9ZM187,91.3a6.13,6.13,0,0,0-6.6-5.5,6.06,6.06,0,0,0-5.5,6.6A75.26,75.26,0,0,1,106.8,174,76.18,76.18,0,0,1,43,148.1L126.1,65a6.11,6.11,0,0,0,1.2-6.9,6.2,6.2,0,0,0-6.1-3.5L86.4,57.7a6.12,6.12,0,0,0,1.1,12.2l18.1-1.6L30.4,143.4a6.09,6.09,0,0,0-.5,8,89.05,89.05,0,0,0,70.4,35.1c2.5,0,5.1-.1,7.6-0.3A87.56,87.56,0,0,0,187,91.3Z"
                    transform="translate(-12.73 -11.67)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("transfer.batch_transfer") }}</span>
            </button>
            <button
              class="common_btn imgBtn"
              v-bind:class="{
                disabledWriteBtn: !nonDownloading,
                writeBtn: nonDownloading,
              }"
              v-if="whetherShowSendMsgBtn()"
              @click="openSendMessageDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 171.43 137.08"
                >
                  <path
                    class="cls-1"
                    d="M173.41,143.1a10.26,10.26,0,0,1-10.24,10.25H36.84A10.26,10.26,0,0,1,26.59,143.1v-92A10.25,10.25,0,0,1,36.84,40.88H163.16a10.25,10.25,0,0,1,10.24,10.24v92ZM163.16,28.57H36.84A22.57,22.57,0,0,0,14.29,51.12v92a22.57,22.57,0,0,0,22.55,22.55H163.16a22.57,22.57,0,0,0,22.55-22.55v-92A22.57,22.57,0,0,0,163.16,28.57ZM151.88,65L100,94.89,47.26,65a6.31,6.31,0,0,0-8.39,2.31,6.16,6.16,0,0,0,2.31,8.39L100,109.07l58-33.44a6.16,6.16,0,0,0,2.26-8.41,6.33,6.33,0,0,0-8.4-2.25h0Z"
                    transform="translate(-14.29 -28.57)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("account.send_message") }}</span>
            </button>
            <button
              class="common_btn imgBtn"
              v-bind:class="{
                disabledWriteBtn: !nonDownloading,
                writeBtn: nonDownloading,
              }"
              v-if="whetherShowStorageBtn()"
              @click="openStorageFileDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 150 162.5"
                >
                  <path
                    d="M49,73.87H61.21v49.19a8.21,8.21,0,0,0,8.19,8.19h61.2a8.19,8.19,0,0,0,8.19-8.19h0V73.86H151a6.29,6.29,0,0,0,6.36-6.21,6.13,6.13,0,0,0-1.41-3.9,3.49,3.49,0,0,0-.66-0.73l-48.63-42a8.67,8.67,0,0,0-5.91-2.3,8.39,8.39,0,0,0-5.7,2.14L44.72,63a4.49,4.49,0,0,0-.79.87,6.13,6.13,0,0,0-1.32,3.8A6.3,6.3,0,0,0,49,73.87h0Z"
                    transform="translate(-25 -18.75)"
                  ></path>
                  <rect y="150" width="150" height="12.5"></rect>
                  <rect y="127.01" width="150" height="12.5"></rect>
                </svg>
              </span>
              <span>{{ $t("account.storage_file") }}</span>
            </button>
            <button
              class="common_btn imgBtn writeBtn"
              v-if="whetherShowOnChainBtn()"
              @click="openOnChainDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 150 162.5"
                >
                  <path
                    d="M49,73.87H61.21v49.19a8.21,8.21,0,0,0,8.19,8.19h61.2a8.19,8.19,0,0,0,8.19-8.19h0V73.86H151a6.29,6.29,0,0,0,6.36-6.21,6.13,6.13,0,0,0-1.41-3.9,3.49,3.49,0,0,0-.66-0.73l-48.63-42a8.67,8.67,0,0,0-5.91-2.3,8.39,8.39,0,0,0-5.7,2.14L44.72,63a4.49,4.49,0,0,0-.79.87,6.13,6.13,0,0,0-1.32,3.8A6.3,6.3,0,0,0,49,73.87h0Z"
                    transform="translate(-25 -18.75)"
                  ></path>
                  <rect y="150" width="150" height="12.5"></rect>
                  <rect y="127.01" width="150" height="12.5"></rect>
                </svg>
              </span>
              <span>{{ $t("account.on_chain") }}</span>
            </button>
            <button
              class="common_btn imgBtn writeBtn"
              v-if="whetherShowJoinNetBtn()"
              @click="openJoinNetDialog"
            >
              <div>
                <img
                  src="../../assets/img/join_net.svg"
                  style="vertical-align: middle"
                  hspace="5"
                  width="18"
                  v-if="projectName === 'mw'"
                />
                <img
                  src="../../assets/img/sharder/join_net.svg"
                  style="vertical-align: middle"
                  hspace="5"
                  width="18"
                  v-else-if="projectName === 'sharder'"
                />
                {{ $t("joinNet.join") }}
              </div>
            </button>
            <!-- Setting Hub Button -->
            <button
              class="common_btn imgBtn writeBtn"
              v-if="whetherShowHubSettingBtn()"
              @click="openHubSettingDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 191.64 181.04"
                >
                  <path
                    d="M-382,127.83h0v0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.23,147l-0.07,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-363.68,147h-0.08Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-227.43,189.31l-0.09.08,0,0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-296.8,31l0,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.36,81h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("account.hub_setting") }}</span>
            </button>
            <!-- Init Hub Button -->
            <button
              class="common_btn imgBtn"
              v-if="whetherShowHubInitBtn()"
              @click="openHubInitDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 191.64 181.04"
                >
                  <path
                    d="M-382,127.83h0v0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.23,147l-0.07,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-363.68,147h-0.08Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-227.43,189.31l-0.09.08,0,0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-296.8,31l0,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.36,81h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("login.init_hub") }}</span>
            </button>
            <!-- Use NAT service button -->
            <button
              class="common_btn imgBtn"
              v-if="whetherShowUseNATServiceBtn()"
              @click="openUseNATDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 191.64 181.04"
                >
                  <path
                    d="M-382,127.83h0v0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.23,147l-0.07,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-363.68,147h-0.08Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-227.43,189.31l-0.09.08,0,0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-296.8,31l0,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.36,81h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("login.use_nat_server") }}</span>
            </button>
            <!-- Configure NAT service button -->
            <button
              class="common_btn imgBtn"
              v-if="whetherShowConfigureNATServiceBtn()"
              @click="openUseNATDialog"
            >
              <span class="icon">
                <svg
                  fill="#fff"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 191.64 181.04"
                >
                  <path
                    d="M-382,127.83h0v0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.23,147l-0.07,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-363.68,147h-0.08Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-227.43,189.31l-0.09.08,0,0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-287,81.4A32.6,32.6,0,0,0-319.61,114,32.63,32.63,0,0,0-287,146.6,32.64,32.64,0,0,0-254.37,114,32.63,32.63,0,0,0-287,81.4Zm0,51.2A18.64,18.64,0,0,1-305.61,114,18.63,18.63,0,0,1-287,95.4,18.64,18.64,0,0,1-268.37,114,18.66,18.66,0,0,1-287,132.6Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-192,100.14a24.25,24.25,0,0,0-6.37-12.19,24.2,24.2,0,0,0-11.94-7l-2.08-.48a12.38,12.38,0,0,1-6.89-5.6A12.55,12.55,0,0,1-221,68.66a12.42,12.42,0,0,1,.24-2.45l0.62-1.85,0.1-.31a22.6,22.6,0,0,0,1-6.65,25.77,25.77,0,0,0-2-9.91,23.67,23.67,0,0,0-6.38-8.83h0c-0.81-.69-4.05-3.27-11.62-7.64l0,0a88.68,88.68,0,0,0-12.22-6.15h0a22.73,22.73,0,0,0-7.83-1.35A24.92,24.92,0,0,0-277.14,31l0,0-1.52,1.62A12.25,12.25,0,0,1-287,35.78a12.44,12.44,0,0,1-8.36-3.19L-296.78,31a24.82,24.82,0,0,0-18-7.57,22.73,22.73,0,0,0-7.84,1.35h0A87,87,0,0,0-334.88,31L-335,31a83.41,83.41,0,0,0-11.58,7.67,23.6,23.6,0,0,0-6.35,8.77,25.67,25.67,0,0,0-2,9.93A22.81,22.81,0,0,0-354,64v0l0.62,2a13,13,0,0,1,.27,2.63,12.39,12.39,0,0,1-1.68,6.25,12.41,12.41,0,0,1-6.89,5.6l-2,.46h0a24.24,24.24,0,0,0-11.91,6.91A24.16,24.16,0,0,0-382,100.19v0a84.4,84.4,0,0,0-.8,13.84c0,8.71.61,12.78,0.8,13.83a24.2,24.2,0,0,0,6.38,12.23A24.16,24.16,0,0,0-363.71,147l2,0.45a12.39,12.39,0,0,1,7,5.62v0a12.42,12.42,0,0,1,1.7,6.27,12.35,12.35,0,0,1-.28,2.62l-0.6,2a22.62,22.62,0,0,0-1,6.59,25.77,25.77,0,0,0,2,9.92,23.67,23.67,0,0,0,6.37,8.83l0,0a84,84,0,0,0,11.61,7.63,84.21,84.21,0,0,0,12.37,6.22,22.74,22.74,0,0,0,7.76,1.33A24.91,24.91,0,0,0-296.83,197l0,0,1.39-1.5a12.58,12.58,0,0,1,8.41-3.22,12.45,12.45,0,0,1,8.4,3.23l1.42,1.52,0,0a24.83,24.83,0,0,0,18,7.57,22.76,22.76,0,0,0,7.83-1.35h0A86.42,86.42,0,0,0-239.09,197l0,0a84.17,84.17,0,0,0,11.53-7.59,23.48,23.48,0,0,0,6.44-8.86,25.75,25.75,0,0,0,2-9.92,22.71,22.71,0,0,0-1-6.61l0,0.06-0.63-2.13a12.69,12.69,0,0,1-.27-2.62,12.2,12.2,0,0,1,1.66-6.18v0a12.45,12.45,0,0,1,7-5.65l2-.46a24.21,24.21,0,0,0,11.9-6.9A24.18,24.18,0,0,0-192,127.85h0a84.88,84.88,0,0,0,.8-13.82V114A86.79,86.79,0,0,0-192,100.14Zm-13.74,25.23a10.89,10.89,0,0,1-7.64,8l-2.5.58a26.46,26.46,0,0,0-15.46,12.19,26.35,26.35,0,0,0-2.82,19.37l0.77,2.57a10.89,10.89,0,0,1-3.11,10.61,72.25,72.25,0,0,1-9.53,6.19A76.54,76.54,0,0,1-256.19,190a10.87,10.87,0,0,1-10.75-2.6l-1.77-1.89A26.46,26.46,0,0,0-287,178.22a26.51,26.51,0,0,0-18.29,7.27l-1.77,1.89a10.87,10.87,0,0,1-10.75,2.6,73.91,73.91,0,0,1-10.11-5.17,73.79,73.79,0,0,1-9.56-6.19A11,11,0,0,1-340.57,168l0.74-2.47a26.46,26.46,0,0,0-2.82-19.47A26.39,26.39,0,0,0-358.1,133.9l-2.5-.58a10.84,10.84,0,0,1-7.63-8,74.14,74.14,0,0,1-.58-11.35,74.19,74.19,0,0,1,.58-11.35,10.88,10.88,0,0,1,7.63-8l2.57-.58a26.37,26.37,0,0,0,15.43-12.15,26.53,26.53,0,0,0,2.82-19.43l-0.77-2.54a10.86,10.86,0,0,1,3.11-10.58,72.75,72.75,0,0,1,9.53-6.22A76.66,76.66,0,0,1-317.79,38,10.86,10.86,0,0,1-307,40.57l1.8,1.93A26.42,26.42,0,0,0-287,49.78a26.34,26.34,0,0,0,18.19-7.21l1.86-2A10.88,10.88,0,0,1-256.2,38a78.92,78.92,0,0,1,10.1,5.16,73.52,73.52,0,0,1,9.56,6.19,10.92,10.92,0,0,1,3.11,10.61l-0.83,2.51a26.43,26.43,0,0,0,2.83,19.47A26.45,26.45,0,0,0-216.09,94l2.66,0.61a10.9,10.9,0,0,1,7.63,8,76.56,76.56,0,0,1,.62,11.38A74.19,74.19,0,0,1-205.76,125.37Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-296.8,31l0,0h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                  <path
                    d="M-210.36,81h0Z"
                    transform="translate(382.82 -23.48)"
                  ></path>
                </svg>
              </span>
              <span>{{ $t("login.config_nat_server") }}</span>
            </button>

            <button
              class="common_btn imgBtn exchange"
              v-bind:class="{
                disabledWriteBtn: !nonDownloading,
                writeBtn: nonDownloading,
                disabledExchangeBtn: exchangeOpenButton,
              }"
              v-if="whetherShowAssetsAcrossChainsBtn()"
              @click="openAssetsAcrossChainsDialog"
              style="width: 135px"
            >
              <span class="icon" style="color: #a6a9ad">
                <svg
                  version="1.1"
                  id="图层_1"
                  xmlns="http://www.w3.org/2000/svg"
                  xmlns:xlink="http://www.w3.org/1999/xlink"
                  x="0px"
                  y="0px"
                  viewBox="0 0 150 162.5"
                  style="enable-background: new 0 0 150 162.5"
                  xml:space="preserve"
                >
                  <path
                    class="st0"
                    d="M131.1,13.7c6.4,0,11.8,5.3,11.8,11.8v111.9c0,6.4-5.3,11.8-11.8,11.8h-112c-6.4,0-11.8-5.3-11.8-11.8V25.5
                                    c0-6.4,5.3-11.8,11.8-11.8H131.1 M131.1,6.6h-112C8.8,6.6,0.3,15.2,0.3,25.5v111.9c0,10.3,8.6,18.9,18.9,18.9h112
                                    c10.3,0,18.9-8.5,18.9-18.9V25.5C149.7,15.2,141.1,6.6,131.1,6.6L131.1,6.6z M131.1,6.6"
                  />
                  <rect
                    x="0.3"
                    y="6.5"
                    class="st1"
                    width="149.9"
                    height="149.9"
                  />
                  <path
                    class="st0"
                    d="M109.7,62.9H51.6l13.2-13.2c1.1-1.1,1.1-2.5,0-3.2l-3.2-3.2c-0.4-1.1-2.5-1.1-3.2,0L38.8,62.9
                                    c-0.4,0.4-0.7,0.7-0.7,1.4c-0.4,0.4-0.4,0.7-0.4,1.1V70c0,1.4,1.1,2.5,2.5,2.5h70.3c1.4,0,2.5-1.1,2.5-2.5v-4.6
                                    C112.2,63.6,111.2,62.9,109.7,62.9L109.7,62.9z M109.7,62.9"
                  />
                  <path
                    class="st0"
                    d="M110.5,81.4H40.2c-1.4,0-2.5,1.1-2.5,1.8v4.6c0,1.4,0,2.9,4.6,2.9h54.9l-11.8,11.4c-1.1,1.1-1.1,2.5,0,3.2
                                    l3.2,3.2c1.1,1.1,2.5,1.1,3.2,0l19.3-18.2c1.4-0.7,1.4-1.8,1.4-2.1v-4.6C112.2,82.5,111.2,81.4,110.5,81.4L110.5,81.4z M110.5,81.4"
                  />
                </svg>
              </span>
              <span>{{ $t("account.assets_across_chains") }}</span>
            </button>
          </div>
        </div>
      </div>
      <div class="block_receiptDisbursement mb20">
        <p class="block_title">
          <img
            src="../../assets/img/receipt&disbursementInfo.svg"
            v-if="projectName === 'mw'"
          />
          <img
            src="../../assets/img/sharder/receipt&disbursementInfo.svg"
            v-else-if="projectName === 'sharder'"
          />
          <span>{{ $t("account.income_and_expenditure_details") }}</span>
        </p>
        <div class="w">
          <div class="whf" id="transaction_amount_bar"></div>
          <div class="whf" id="yield_curve"></div>
        </div>
      </div>
      <div class="block_list">
        <p class="block_title fl">
          <img
            src="../../assets/img/transaction.svg"
            alt="transactionImg"
            v-if="projectName === 'mw'"
          />
          <img
            src="../../assets/img/sharder/transaction.svg"
            alt="transactionImg"
            v-else-if="projectName === 'sharder'"
          />
          <span>{{ $t("transaction.transaction_record") }}</span>
        </p>
        <div class="transaction_type">
          <span
            class="btn"
            :class="activeSelectType(0)"
            @click="selectType = 0"
          >
            {{ $t("transaction.transaction_type_payment") }}
          </span>
          <span class="btn" :class="activeSelectType(8)" @click="selectType = 8">
            {{ $t("transaction.transaction_type_forge_pool") }}
          </span>
          <span
            class="btn"
            :class="activeSelectType(9)"
            @click="selectType = 9"
          >
            {{ $t("transaction.transaction_type_system_reward") }}
          </span>

          <el-select v-model="selectType" :placeholder="$t('transaction.transaction_type_all')">
            <el-option v-for="item in transactionType" :key="item.value" :label="item.label" :value="item.value">
            </el-option>
          </el-select>
          <el-select class="exchange" id="exchangeSelectType" v-model="exchangeSelectType" :placeholder="$t('transaction.transaction_type_exchange')">
            <el-option v-for="item in exchangeTransactionType" :key="item.value" :label="item.label" :value="item.value">
            </el-option>
          </el-select>
        </div>
        <div class="list_table w br4" v-loading="loading">
          <div class="list_content data_loading">
            <table class="table table_striped" id="blocks_table">
              <thead>
                <tr>
                  <th>{{ $t("transaction.transaction_time") }}</th>
                  <th>{{ $t("transaction.block_height") }}</th>
                  <th>{{ $t("transaction.transaction_id") }}</th>
                  <th class="pc-table">
                    {{ $t("transaction.transaction_type") }}
                  </th>
                  <th class="pc-table">
                    {{ $t("transaction.transaction_amount") }}
                  </th>
                  <th class="pc-table">
                    {{ $t("transaction.transaction_fee") }}
                  </th>
                  <th class="dbw w300 pc-table">
                    {{ $t("transaction.transaction_account") }}
                  </th>
                  <th class="pc-table">
                    {{ $t("transaction.transaction_confirm_quantity") }}
                  </th>
                  <th class="pc-table">{{ $t("transaction.operating") }}</th>
                  <th class="mobile"></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(transaction, index) in accountTransactionList"
                  @click="mobileViewDetail(transaction.transaction)"
                >
                  <td class="tc pl0">
                    <span>{{
                      $global.myFormatTime(
                        transaction.timestamp,
                        "YMDHMS",
                        true
                      )
                    }}</span
                    ><br />
                    <span class="utc-time"
                      >{{
                        $global.formatTime(transaction.timestamp)
                      }}
                      +UTC</span
                    >
                  </td>
                  <td
                    class="linker"
                    @click="openBlockInfoDialog(transaction.height)"
                  >
                    {{
                      $global.returnObj(transaction.block, transaction.height)
                    }}
                  </td>
                  <td>{{ transaction.transaction }}</td>
                  <td
                    class="transaction-img pc-table"
                    v-if="projectName === 'mw'"
                  >
                    <span
                      class="bg"
                      :class="'type' + transaction.type + transaction.subtype"
                    ></span>
                    <span>{{
                      $global.getTransactionTypeStr(transaction)
                    }}</span>
                  </td>
                  <td
                    class="transaction-img-sharder pc-table"
                    v-else-if="projectName === 'sharder'"
                  >
                    <span
                      class="bg"
                      :class="'type' + transaction.type + transaction.subtype"
                    ></span>
                    <span>{{
                      $global.getTransactionTypeStr(transaction)
                    }}</span>
                  </td>
                  <td class="pc-table">
                    {{
                      $global.getTransactionAmountNQT(
                        transaction,
                        accountInfo.accountRS
                      )
                    }}
                  </td>
                  <td class="pc-table">
                    {{ $global.getTransactionFeeNQT(transaction) }}
                  </td>
                  <td class="image_text w300 pc-table">
                    <!-- tx sender -->
                    <span
                      class="linker"
                      v-if="transaction.type === 9 || transaction.type === 18"
                      >Coinbase</span
                    >
                    <span
                      class="linker"
                      v-else-if="transaction.type === 12"
                      @click="openBlockInfoDialog(transaction.height)"
                      >System</span
                    >
                    <span
                      class="linker"
                      @click="openAccountInfoDialog(transaction.senderRS)"
                      v-else-if="
                        transaction.senderRS === accountInfo.accountRS &&
                        transaction.type !== 9
                      "
                    >
                      <span
                        class="linker"
                        @click="openAccountInfoDialog(transaction.recipientRS)"
                        v-if="
                          transaction.type === 8 &&
                          transaction.subtype === 3 &&
                          transaction.recipientRS !== accountInfo.accountRS
                        "
                      >
                        {{ transaction.recipientRS }}
                      </span>
                      <span v-else>{{ $t("transaction.self") }}</span>
                    </span>
                    <span
                      class="linker"
                      v-else-if="
                        transaction.senderRS !== accountInfo.accountRS &&
                        transaction.type !== 9
                      "
                    >
                      <span
                        v-if="
                          transaction.type === 8 && transaction.subtype === 3
                        "
                      >
                        {{ $t("transaction.self") }}
                      </span>
                      <span v-else>{{ transaction.senderRS }}</span>
                    </span>

                    <img
                      src="../../assets/img/right_arrow.svg"
                      v-if="projectName === 'mw'"
                    />
                    <img
                      src="../../assets/img/sharder/right_arrow.svg"
                      v-else-if="projectName === 'sharder'"
                    />
                    <!-- tx recipient -->
                    <span
                      class="linker"
                      @click="openAccountInfoDialog(transaction.senderRS)"
                      v-if="transaction.type === 9"
                    >
                      {{ $t("transaction.self") }}
                    </span>
                    <span v-else-if="transaction.type === 18">
                      <span
                        class="linker"
                        @click="openAccountInfoDialog(transaction.recipientRS)"
                      >
                        {{ $t("transaction.transaction_burn_account") }}
                      </span>
                    </span>
                    <span
                      class="linker"
                      v-else-if="
                        transaction.type === 8 && transaction.subtype === 3
                      "
                    >
                      <span
                        class="linker"
                        @click="openAccountInfoDialog(transaction.recipientRS)"
                        v-if="
                          transaction.recipientRS !== accountInfo.accountRS &&
                          transaction.type !== 9 &&
                          transaction.senderRS === accountInfo.accountRS
                        "
                      >
                        {{ $t("transaction.transaction_type_forge_pool") }}:{{
                          transaction.attachment.poolId
                        }}
                      </span>
                      <span class="linker" v-else
                        >{{ $t("transaction.transaction_type_forge_pool") }}:{{
                          transaction.attachment.poolId
                        }}</span
                      >
                    </span>
                    <span
                      class="linker"
                      v-else-if="
                        transaction.type === 8 && transaction.subtype === 2
                      "
                    >
                      {{ $t("transaction.transaction_type_forge_pool") }}:{{
                        transaction.attachment.poolId
                      }}
                    </span>
                    <span
                      class="linker"
                      v-else-if="
                        transaction.type === 8 && transaction.subtype === 1
                      "
                    >
                      {{ $t("transaction.transaction_type_forge_pool") }}:{{
                        transaction.attachment.poolId
                      }}
                    </span>
                    <span
                      class="linker"
                      @click="openAccountInfoDialog(transaction.recipientRS)"
                      v-else-if="
                        transaction.recipientRS === accountInfo.accountRS &&
                        transaction.type !== 9
                      "
                    >
                      {{ $t("transaction.self") }}
                    </span>
                    <span
                      class="linker"
                      v-else-if="typeof transaction.recipientRS === 'undefined'"
                      >/</span
                    >
                    <span
                      class="linker"
                      @click="openAccountInfoDialog(transaction.recipientRS)"
                      v-else-if="
                        transaction.recipientRS !== accountInfo.accountRS &&
                        transaction.type !== 9
                      "
                    >
                      {{ transaction.recipientRS }}
                    </span>
                  </td>
                  <td class="pc-table">
                    {{
                      $global.returnObj(
                        transaction.block,
                        transaction.confirmations
                      )
                    }}
                  </td>
                  <td
                    class="linker pc-table"
                    @click="openTradingInfoDialog(transaction.transaction)"
                  >
                    {{ $t("transaction.view_details") }}
                  </td>
                  <td class="mobile icon-box">
                    <i class="el-icon-arrow-right"></i>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="list_pagination">
            <!--v-if="totalSize > pageSize">-->
            <div class="list_pagination">
              <el-pagination
                :small="isMobile"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page.sync="currentPage"
                :page-size="pageSize"
                layout="total, prev, pager, next, jumper"
                :total="totalSize"
              >
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
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">
              {{ $t("sendMessage.sendMessage_title") }}
            </h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item
                :label="$t('sendMessage.receiver')"
                class="item_receiver"
              >
                <masked-input
                  id="receiver"
                  mask="AAA-****-****-****-*****"
                  v-model="messageForm.receiver"
                />
                <img
                  src="../../assets/img/account_directory.svg"
                  v-if="projectName === 'mw'"
                />
                <img
                  src="../../assets/img/sharder/account_directory.svg"
                  v-else-if="projectName === 'sharder'"
                />
              </el-form-item>
              <el-form-item
                :label="$t('sendMessage.receiver_publickey')"
                v-if="messageForm.hasPublicKey"
              >
                <el-input
                  v-model="messageForm.publicKey"
                  type="password"
                ></el-input>
              </el-form-item>
              <el-form-item :label="$t('sendMessage.infomation')">
                <el-checkbox v-model="messageForm.isEncrypted">
                  {{ $t("sendMessage.encrypted_information") }}
                </el-checkbox>
                <el-input
                  :disabled="messageForm.isFile"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 10 }"
                  resize="none"
                  :placeholder="$t('sendMessage.message_tip')"
                  v-model="messageForm.message"
                >
                </el-input>
              </el-form-item>
              <el-form-item :label="$t('sendMessage.file')">
                <el-input
                  :placeholder="$t('sendMessage.file_tip')"
                  class="input-with-select"
                  v-model="messageForm.fileName"
                  :readonly="true"
                >
                  <el-button slot="append" v-if="file === null"
                    >{{ $t("sendMessage.browse") }}
                  </el-button>
                  <el-button slot="append" @click="delFile" v-else
                    >{{ $t("sendMessage.delete") }}
                  </el-button>
                </el-input>
                <input
                  id="file"
                  ref="file"
                  type="file"
                  @change="fileChange"
                  v-if="file === null"
                />
              </el-form-item>
              <el-form-item :label="$t('sendMessage.fee')">
                <el-button class="calculate_fee" @click="getMessageFee()">
                  {{ $t("sendMessage.calc_short") }}
                </el-button>
                <input
                  class="el-input__inner"
                  v-model="messageForm.fee"
                  type="number"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item
                :label="$t('sendMessage.secret_key')"
                v-if="!secretPhrase"
              >
                <el-input
                  v-model="messageForm.password"
                  type="password"
                ></el-input>
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              v-loading="messageForm.executing"
              class="btn common_btn writeBtn"
              @click="sendMessageInfo"
              :disabled="messageForm.executing"
            >
              {{ $t("sendMessage.send_message") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!--view storage file dialog-->
    <div class="modal" id="storage_file_modal" v-show="storageFileDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">
              {{ $t("sendMessage.upload_file_title") }}
            </h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item :label="$t('sendMessage.file')">
                <el-input
                  :placeholder="$t('sendMessage.file_tip')"
                  class="input-with-select"
                  v-model="messageForm.fileName"
                  :readonly="true"
                >
                  <el-button slot="append" v-if="storagefile === null"
                    >{{ $t("sendMessage.browse") }}
                  </el-button>
                  <el-button slot="append" @click="delStorageFile" v-else>
                    {{ $t("sendMessage.delete") }}
                  </el-button>
                </el-input>
                <input
                  id="storageFile"
                  ref="storageFile"
                  type="file"
                  @change="storageFileChange"
                  v-if="storagefile === null"
                />
              </el-form-item>
              <el-form-item :label="$t('sendMessage.fee')">
                <el-button class="calculate_fee" @click="getMessageFee()">
                  {{ $t("sendMessage.calc_short") }}
                </el-button>
                <input
                  class="el-input__inner"
                  v-model="messageForm.fee"
                  type="number"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item
                :label="$t('sendMessage.secret_key')"
                v-if="!secretPhrase"
              >
                <el-input
                  v-model="messageForm.password"
                  type="password"
                ></el-input>
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn common_btn writeBtn"
              @click="uploadFile"
              :disabled="isDisable"
            >
              {{ $t("sendMessage.upload_file") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!--view on chain file dialog-->
    <div class="modal" id="on_chain_file_modal" v-show="onChainDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">
              {{ $t("sendMessage.upload_file_title") }}
            </h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item :label="$t('sendMessage.file')">
                <el-input
                  :placeholder="$t('sendMessage.file_tip')"
                  class="input-with-select"
                  v-model="messageForm.fileName"
                  :readonly="true"
                >
                  <el-button slot="append" v-if="onchainfile === null"
                    >{{ $t("sendMessage.browse") }}
                  </el-button>
                  <el-button slot="append" @click="delOnChainFile" v-else>
                    {{ $t("sendMessage.delete") }}
                  </el-button>
                </el-input>
                <input
                  id="onChainFile"
                  ref="onChainFile"
                  type="file"
                  @change="onChainFileChange"
                  v-if="onchainfile === null"
                />
              </el-form-item>
              <el-form-item :label="$t('sendMessage.fee')">
                <el-button class="calculate_fee" @click="getMessageFee()">
                  {{ $t("sendMessage.calc_short") }}
                </el-button>
                <input
                  class="el-input__inner"
                  v-model="messageForm.fee"
                  type="number"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item
                :label="$t('sendMessage.secret_key')"
                v-if="!secretPhrase"
              >
                <el-input
                  v-model="messageForm.password"
                  type="password"
                ></el-input>
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn common_btn writeBtn"
              @click="onChain"
              :disabled="isDisable"
            >
              {{ $t("sendMessage.upload_file") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!--view join net dialog-->
    <div class="modal" id="join_net_modal" v-show="joinNetDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">{{ $t("joinNet.joinNet") }}</h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item :label="$t('joinNet.disk_capacity')">
                <el-slider
                  v-model="capacity"
                  :format-tooltip="formatInputDiskCapacity"
                  :max="maxCapacity"
                  @change="calculatePledge"
                ></el-slider>
              </el-form-item>
              <el-form-item :label="$t('joinNet.mortgage')">
                <input
                  class="el-input__inner"
                  v-model="diskPledge"
                  @blur="calPledgeCapacityByBalance"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item :label="$t('joinNet.privateKey')">
                <el-input
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 4 }"
                  :placeholder="$t('joinNet.inputPK')"
                  v-model="accountSecret"
                />
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn common_btn writeBtn"
              @click="joinNet"
              :disabled="isDisable"
            >
              {{ $t("joinNet.joinNet") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- view transfer account dialog -->
    <div class="modal" id="transfer_accounts_modal" v-show="tranferAccountsDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">{{ $t("transfer.transfer_title") }}</h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
              >
                <masked-input
                  id="tranfer_receiver"
                  mask="AAA-****-****-****-*****"
                  v-model="transfer.receiver"
                />
                <img
                  src="../../assets/img/account_directory.svg"
                  v-if="projectName === 'mw'"
                />
                <img
                  src="../../assets/img/sharder/account_directory.svg"
                  v-else-if="projectName === 'sharder'"
                />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver_public_key')"
                v-if="transfer.hasPublicKey"
              >
                <el-input
                  v-model="transfer.publicKey"
                  type="password"
                ></el-input>
              </el-form-item>
              <el-form-item :label="$t('transfer.amount')">
                <input
                  class="el-input__inner"
                  v-model="transfer.number"
                  type="number"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item :label="$t('transfer.fee')">
                <el-button class="calculate_fee" @click="getTransferFee()">
                  {{ $t("transfer.calc_short") }}
                </el-button>
                <input
                  class="el-input__inner"
                  v-model="transfer.fee"
                  type="number"
                />
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item label="">
                <el-checkbox v-model="transfer.hasMessage"
                  >{{ $t("transfer.enable_add_info") }}
                </el-checkbox>
                <el-checkbox
                  ref="encrypted2"
                  v-model="transfer.isEncrypted"
                  :disabled="!transfer.hasMessage"
                  >{{ $t("transfer.encrypted_information") }}
                </el-checkbox>
                <el-input
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 10 }"
                  resize="none"
                  :placeholder="$t('transfer.message_tip')"
                  v-model="transfer.message"
                  :disabled="!transfer.hasMessage"
                >
                </el-input>
              </el-form-item>
              <el-form-item
                :label="$t('transfer.secret_key')"
                v-if="!secretPhrase"
              >
                <el-input
                  v-model="transfer.password"
                  type="password"
                ></el-input>
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              v-loading="transfer.executing"
              class="btn common_btn writeBtn"
              @click="sendTransferInfo"
              :disabled="transfer.executing"
            >
              {{ $t("transfer.transfer_send") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- view transfer account dialog -->
    <div class="modal" id="batch_transfer_accounts_modal" v-show="batchTranferAccountsDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">
              {{ $t("transfer.batch_transfer_title") }}
            </h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item :label="$t('sendMessage.json_file')">
                <el-input
                  :placeholder="$t('sendMessage.json_file_tip')"
                  class="input-with-select"
                  v-model="batch_transfer.fileName"
                  :readonly="true"
                >
                  <el-button slot="append" v-if="parsefile === null"
                    >{{ $t("sendMessage.browse") }}
                  </el-button>
                  <el-button slot="append" @click="delParseFile" v-else
                    >{{ $t("sendMessage.delete") }}
                  </el-button>
                </el-input>
                <input
                  id="parseFile"
                  ref="parseFile"
                  type="file"
                  accept="application/json"
                  @change="parseFileChange"
                  v-if="parsefile === null"
                />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.secret_key')"
                v-if="!secretPhrase"
              >
                <el-input
                  v-model="batch_transfer.password"
                  type="password"
                ></el-input>
              </el-form-item>
              <el-form-item :label="$t('transfer.airdrop_secret_key')">
                <el-input
                  v-model="batch_transfer.airdropSecretKey"
                  type="password"
                ></el-input>
              </el-form-item>
              <el-form-item>
                <el-row>
                  <el-col :span="24"
                    ><span>{{
                      $t("transfer.airdrop_description")
                    }}</span></el-col
                  >
                </el-row>
              </el-form-item>
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              v-loading="batch_transfer.executing"
              class="btn common_btn writeBtn"
              @click="sendBatchTransferInfo"
              :disabled="batch_transfer.executing"
            >
              {{ $t("transfer.batch_transfer_send") }}
            </button>
            <el-row>
              <el-col :span="24">
                <div style="text-align: center; margin: 5px auto">or</div>
              </el-col>
            </el-row>
            <button
              type="button"
              v-loading="batch_transfer.executingAnother"
              class="btn common_btn writeBtn"
              @click="detectionBatchTransferInfo"
              :disabled="batch_transfer.executingAnother"
            >
              {{ $t("transfer.batch_transfer_detection") }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- view client init setting dialog -->
    <div class="modal_hubSetting" id="hub_init_setting" v-show="hubInitDialog">
      <div class="modal-header" @click="displaySerialNo('initial')">
        <h4 class="modal-title">
          <span>{{ $t("login.init_hub") }}</span>
        </h4>
      </div>
      <div class="modal-body">
        <el-form
          label-position="left"
          style="max-height: 500px; overflow: auto"
          v-loading="hubsetting.loadingData"
          :model="hubsetting"
          status-icon
          :rules="formRules"
          :label-width="this.$i18n.locale === 'en' ? '200px' : '160px'"
          ref="initForm"
        >
          <!--                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">-->
          <!--                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>-->
          <!--                    </el-form-item>-->
          <el-form-item
            :label="$t('hubsetting.token_address')"
            v-if="userConfig.ssAddress"
          >
            <el-input
              v-model="userConfig.ssAddress"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.serial_no')"
            v-show="userConfig.xxx && hubsetting.initialSerialClickCount >= 5"
          >
            <el-input v-model="userConfig.xxx" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.disk_capacity')"
            v-if="userConfig.diskCapacity > 0"
          >
            <el-input
              v-model="this.formatDiskCapacity()"
              :disabled="true"
            ></el-input>
          </el-form-item>

          <!-- register the new site account                   -->
          <!--                    <el-form-item :label="$t('hubsetting.register_sharder_account')">-->
          <!--                        <el-switch-->
          <!--                            v-model="hubsetting.registerSiteAccount"-->
          <!--                            active-color="#13ce66"-->
          <!--                            inactive-color="#ff4949">-->
          <!--                        </el-switch>-->
          <!--                    </el-form-item>-->
          <!--                    <el-divider  v-if="hubsetting.registerSiteAccount"><i class="el-icon-caret-top"></i></el-divider>-->

          <!--                    <el-form-item :label="$t('hubsetting.sharder_account_phone_or_email')"  v-if="hubsetting.registerSiteAccount">-->
          <!--                        <el-input v-model="registerSharderSiteUser.sharderAccountPhoneOrEmail" ></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.verification_code')" v-if="hubsetting.registerSiteAccount">-->
          <!--                        <el-input v-model="registerSharderSiteUser.verificationCode" ></el-input><el-button type="primary" style="position: absolute;right:0px;top:0px " @click="sendVCode()" :disabled="sendSuccess">-->
          <!--                        {{sendSuccess?time+"s 可"+$t('hubsetting.resend_verification_code'):$t('hubsetting.send_verification_code')}}-->
          <!--                    </el-button>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.set_sharder_account_password')"  v-if="hubsetting.registerSiteAccount">-->
          <!--                        <el-input type="password" v-model="registerSharderSiteUser.setSharderPwd"></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.confirm_sharder_account_password')"  v-if="hubsetting.registerSiteAccount">-->
          <!--                        <el-input type="password" v-model="registerSharderSiteUser.confirmSharderPwd"></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.picture_verification_code')"  v-if="hubsetting.registerSiteAccount">-->
          <!--                        <el-input v-model="registerSharderSiteUser.pictureVerificationCode" @blur="checkPicVerificationCode"></el-input>-->
          <!--                        <el-image :src="src" style="width:112px;height:38px;position: absolute;right:1px;top:1px " @click="getPicVCode()">-->
          <!--                            <div slot="placeholder" class="image-slot">-->
          <!--                                Loding<span class="dot">...</span>-->
          <!--                            </div>-->
          <!--                        </el-image>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item >-->
          <!--                        <el-button type="primary" v-if="hubsetting.registerSiteAccount" style="float: right" @click="registerSharderSite()">-->
          <!--                            {{$t('hubsetting.register_sharder_account')}}-->
          <!--                        </el-button>-->
          <!--                    </el-form-item>-->
          <!--                    <el-divider  v-if="hubsetting.registerSiteAccount"><i class="el-icon-caret-bottom"></i></el-divider>-->

          <!-- quick auth                   -->
          <el-form-item
            :label="$t('hubsetting.quick_auth')"
            v-if="userConfig.permissionModeDisplay"
          >
            <el-switch
              v-model="userConfig.permissionMode"
              active-color="#13ce66"
              inactive-color="#ff4949"
            >
            </el-switch>
          </el-form-item>
          <el-divider v-if="userConfig.permissionMode"
            ><i class="el-icon-caret-top"></i
          ></el-divider>

          <el-form-item
            :label="$t('hubsetting.sharder_account')"
            prop="sharderAccount"
            v-if="!hubsetting.registerSiteAccount && userConfig.permissionMode"
          >
            <el-input
              v-model="userConfig.siteAccount"
              :placeholder="$t('hubsetting.sharder_account_des')"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.sharder_account_password')"
            prop="sharderPwd"
            v-if="!hubsetting.registerSiteAccount && userConfig.permissionMode"
          >
            <el-input
              type="password"
              v-model="hubsetting.sharderPwd"
              @blur="checkSiteAccount"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.factory_num')"
            prop="factoryNum"
            v-if="userConfig.permissionMode"
          >
            <el-input
              v-model="userConfig.factoryNum"
              :placeholder="$t('hubsetting.factory_des')"
            ></el-input>
          </el-form-item>
          <el-divider v-if="userConfig.permissionMode"
            ><i class="el-icon-caret-bottom"></i
          ></el-divider>

          <!-- NAT Seeting by site account -->
          <!--                    <el-form-item :label="$t('hubsetting.nat_traversal_address')" v-if="hubsetting.openPunchthrough"-->
          <!--                                  prop="address">-->
          <!--                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.nat_traversal_port')" v-if="hubsetting.openPunchthrough"-->
          <!--                                  prop="port">-->
          <!--                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.nat_traversal_clent_privateKey')"-->
          <!--                                  v-if="hubsetting.openPunchthrough" prop="clientSecretkey">-->
          <!--                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>-->
          <!--                    </el-form-item>-->
          <!--                    <el-form-item :label="$t('hubsetting.public_ip_address')" v-if="hubsetting.openPunchthrough" prop="publicAddress">-->
          <!--                        <el-input v-model="hubsetting.publicAddress" :disabled="hubsetting.openPunchthrough"></el-input>-->
          <!--                    </el-form-item>-->

          <!--<el-form-item class="create_account" :label="$t('hubsetting.token_address')" prop="SS_Address">-->
          <!--<el-input v-model="hubsetting.SS_Address"></el-input>-->
          <!--&lt;!&ndash;<a @click="register"><span>$t('hubsetting.create_account')</span></a>&ndash;&gt;-->
          <!--</el-form-item>-->
          <el-form-item :label="$t('hubsetting.enable_auto_mining')" hidden>
            <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.set_mnemonic_phrase')"
            v-if="hubsetting.isOpenMining"
            prop="modifyMnemonicWord"
          >
            <el-input
              type="password"
              v-model="hubsetting.modifyMnemonicWord"
            ></el-input>
          </el-form-item>
          <el-form-item :label="$t('hubsetting.set_password')" prop="newPwd">
            <el-input type="password" v-model="hubsetting.newPwd"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.confirm_password')"
            prop="confirmPwd"
          >
            <el-input
              type="password"
              v-model="hubsetting.confirmPwd"
            ></el-input>
          </el-form-item>
        </el-form>
        <div class="footer-btn">
          <button
            class="common_btn writeBtn"
            v-loading="hubsetting.executing"
            @click="verifyHubSetting('init')"
            :disabled="hubsetting.executing"
          >
            {{ $t("hubsetting.confirm_restart") }}
          </button>
          <button class="common_btn writeBtn" @click="closeDialog">
            {{ $t("hubsetting.cancel") }}
          </button>
        </div>
      </div>
    </div>
    <!--view hub resetting dialog-->
    <div class="modal_hubSetting" id="hub_setting" v-loading="hubsetting.loadingData" v-show="hubSettingDialog">
      <div class="modal-header" @click="displaySerialNo('setting')">
        <button
          class="common_btn long"
          @click="openAdminDialog('factoryReset')"
        >
          {{ $t("hubsetting.factory_reset") }}
        </button>
        <button class="common_btn" @click="openAdminDialog('reset')">
          {{ $t("hubsetting.reset") }}
        </button>
        <button class="common_btn" @click="openAdminDialog('restart')">
          {{ $t("hubsetting.restart") }}
        </button>

        <h4 class="modal-title">
          <span>{{ $t("hubsetting.title") }}</span>
        </h4>
      </div>
      <div class="modal-body">
        <el-form
          label-position="left"
          label-width="160px"
          :rules="formRules"
          :model="hubsetting"
          v-loading="registerNatLoading"
          ref="reconfigureForm"
          status-icon
        >
          <!--                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">-->
          <!--                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>-->
          <!--                    </el-form-item>-->
          <el-form-item
            :label="$t('hubsetting.token_address')"
            v-if="userConfig.ssAddress"
          >
            <el-input
              v-model="userConfig.ssAddress"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.serial_no')"
            v-show="userConfig.xxx && hubsetting.settingSerialClickCount >= 5"
          >
            <el-input v-model="userConfig.xxx" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.disk_capacity')"
            v-if="userConfig.diskCapacity > 0"
          >
            <el-input
              v-model="this.formatDiskCapacity()"
              :disabled="true"
            ></el-input>
          </el-form-item>

          <el-form-item
            :label="$t('hubsetting.sharder_account')"
            prop="sharderAccount"
            v-if="
              userConfig.permissionModeDisplay &&
              userConfig.nodeType != 'Normal'
            "
          >
            <el-input v-model="userConfig.siteAccount"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.sharder_account_password')"
            prop="sharderPwd"
            v-if="
              userConfig.permissionModeDisplay &&
              userConfig.nodeType != 'Normal'
            "
          >
            <el-input
              type="password"
              v-model="hubsetting.sharderPwd"
              @blur="checkSiteAccount"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_address')"
            v-if="hubsetting.openPunchthrough"
          >
            <el-input v-model="hubsetting.address" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_port')"
            v-if="hubsetting.openPunchthrough"
          >
            <el-input v-model="hubsetting.port" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_clent_privateKey')"
            v-if="hubsetting.openPunchthrough"
          >
            <el-input
              v-model="hubsetting.clientSecretkey"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.public_ip_address')"
            v-if="hubsetting.openPunchthrough"
          >
            <el-input
              v-model="hubsetting.publicAddress"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item :label="$t('hubsetting.enable_auto_mining')" hidden>
            <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.reset_mnemonic_phrase')"
            v-if="hubsetting.isOpenMining"
            prop="modifyMnemonicWord"
          >
            <el-input
              type="password"
              v-model="hubsetting.modifyMnemonicWord"
            ></el-input>
          </el-form-item>
          <el-form-item :label="$t('hubsetting.reset_password')" prop="newPwd">
            <el-input type="password" v-model="hubsetting.newPwd"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.confirm_password')"
            prop="confirmPwd"
          >
            <el-input
              type="password"
              v-model="hubsetting.confirmPwd"
            ></el-input>
          </el-form-item>
        </el-form>
        <div class="footer-btn">
          <button
            class="common_btn writeBtn"
            @click="openAdminDialog('reConfig')"
          >
            {{ $t("hubsetting.confirm_restart") }}
          </button>
          <button class="common_btn writeBtn" @click="closeDialog()">
            {{ $t("hubsetting.cancel") }}
          </button>
        </div>
      </div>
    </div>
    <!-- view use NAT service dialog -->
    <div class="modal_hubSetting" id="use_nat_service" v-show="useNATServiceDialog">
      <div class="modal-header" @click="displaySerialNo('nat')">
        <button
          class="common_btn"
          @click="openAdminDialog('resetNormal')"
          v-if="whetherShowConfigureNATServiceBtn()"
        >
          {{ $t("hubsetting.reset") }}
        </button>
        <button
          class="common_btn"
          @click="openAdminDialog('restart')"
          v-if="whetherShowConfigureNATServiceBtn()"
        >
          {{ $t("hubsetting.restart") }}
        </button>
        <h4 class="modal-title">
          <span>{{ $t("login.use_nat_server") }}</span>
        </h4>
      </div>
      <div class="modal-body">
        <el-form
          label-position="left"
          :model="hubsetting"
          status-icon
          :rules="formRules"
          :label-width="this.$i18n.locale === 'en' ? '200px' : '160px'"
          ref="useNATForm"
        >
          <!--                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">-->
          <!--                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>-->
          <!--                    </el-form-item>-->
          <el-form-item
            :label="$t('hubsetting.token_address')"
            v-if="userConfig.ssAddress && userConfig.ssAddress !== ''"
          >
            <el-input
              v-model="userConfig.ssAddress"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.serial_no')"
            v-show="userConfig.xxx && hubsetting.natSerialClickCount >= 5"
          >
            <el-input v-model="userConfig.xxx" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.disk_capacity')"
            v-if="userConfig.diskCapacity > 0"
          >
            <el-input
              v-model="this.formatDiskCapacity()"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.sharder_account')"
            prop="sharderAccount"
          >
            <el-input v-model="userConfig.siteAccount"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.sharder_account_password')"
            prop="sharderPwd"
          >
            <el-input
              type="password"
              v-model="hubsetting.sharderPwd"
              @blur="checkSiteAccount"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.register_status')"
            v-if="hubsetting.openPunchthrough"
          >
            <el-input
              v-model="hubsetting.register_status_text"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_address')"
            v-if="hubsetting.openPunchthrough"
            prop="address"
          >
            <el-input v-model="hubsetting.address" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_port')"
            v-if="hubsetting.openPunchthrough"
            prop="port"
          >
            <el-input v-model="hubsetting.port" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.nat_traversal_clent_privateKey')"
            v-if="hubsetting.openPunchthrough"
            prop="clientSecretkey"
          >
            <el-input
              v-model="hubsetting.clientSecretkey"
              :disabled="true"
            ></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.public_ip_address')"
            prop="publicAddress"
          >
            <el-input
              v-model="hubsetting.publicAddress"
              :disabled="hubsetting.openPunchthrough"
            ></el-input>
          </el-form-item>
          <!--<el-form-item class="create_account" :label="$t('hubsetting.token_address')"-->
          <!--prop="SS_Address" v-if="!this.needRegister">-->
          <!--<el-input v-model="hubsetting.SS_Address"-->
          <!--:disabled="this.hubsetting.register_status !== 1 && hubsetting.openPunchthrough"></el-input>-->
          <!--</el-form-item>-->
          <!--<el-form-item :label="$t('hubsetting.enable_auto_mining')">-->
          <!--<el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>-->
          <!--</el-form-item>-->
          <el-form-item
            :label="$t('hubsetting.set_mnemonic_phrase')"
            v-if="hubsetting.isOpenMining"
            prop="modifyMnemonicWord"
          >
            <el-input
              type="password"
              v-model="hubsetting.modifyMnemonicWord"
            ></el-input>
          </el-form-item>
          <el-form-item :label="$t('hubsetting.set_password')" prop="newPwd">
            <el-input type="password" v-model="hubsetting.newPwd"></el-input>
          </el-form-item>
          <el-form-item
            :label="$t('hubsetting.confirm_password')"
            prop="confirmPwd"
          >
            <el-input
              type="password"
              v-model="hubsetting.confirmPwd"
            ></el-input>
          </el-form-item>
        </el-form>
        <div class="footer-btn">
          <el-button
            class="common_btn imgBtn"
            @click="verifyHubSetting('register')"
            v-if="!hubsetting.openPunchthrough || !this.needRegister"
            :disabled="
              this.hubsetting.register_status !== 1 &&
              hubsetting.openPunchthrough
            "
            >{{ $t("hubsetting.confirm_restart") }}
          </el-button>
          <el-button
            class="common_btn imgBtn"
            @click="registerNatService()"
            v-if="hubsetting.openPunchthrough && this.needRegister"
            >{{ $t("hubsetting.register_nat_server") }}
          </el-button>
          <button class="common_btn writeBtn" @click="closeDialog">
            {{ $t("hubsetting.cancel") }}
          </button>
        </div>
      </div>
    </div>
    <!--view account transaction dialog-->
    <div class="modal_info" id="account_info" v-show="userInfoDialog">
      <div class="modal-header">
        <img
          class="close"
          src="../../assets/img/error.svg"
          @click="closeDialog"
        />
        <h4 class="modal-title">
          <span>{{ $t("account_info.account_information") }}</span>
        </h4>
      </div>
      <div class="modal-body">
        <table class="table">
          <tbody>
            <tr>
              <th>{{ $t("account_info.accountID") }}</th>
              <td>{{ accountInfo.account }}</td>
            </tr>
            <tr>
              <th>{{ $t("account_info.account_address") }}</th>
              <td>{{ accountInfo.accountRS }}</td>
            </tr>
            <tr>
              <th>{{ $t("account_info.account_name") }}</th>
              <td>
                <div class="accountName" v-if="isShowName">
                  <span v-if="accountInfo.name">{{ accountInfo.name }}</span>
                  <span v-else style="color: #999; font-weight: normal">{{
                    $t("account_info.account_name_not_set")
                  }}</span>
                  <img
                    src="../../assets/img/rewrite.svg"
                    @click="isShowName = false"
                    v-if="projectName === 'mw'"
                  />
                  <img
                    src="../../assets/img/sharder/rewrite.svg"
                    @click="isShowName = false"
                    v-else-if="projectName === 'sharder'"
                  />
                </div>
                <div class="rewriteName" v-else>
                  <el-input v-model="temporaryName"></el-input>
                  <button class="common_btn" @click="openSecretPhraseDialog">
                    {{ $t("account_info.account_set_name") }}
                  </button>
                </div>
              </td>
            </tr>
            <tr>
              <th>
                {{ $t("account_info.account_balance") }}
                <el-tooltip
                  class="item"
                  effect="dark"
                  :content="$t('account_info.account_balance_explain')"
                  placement="top-start"
                >
                  <p class="el-icon-info"></p>
                </el-tooltip>
              </th>
              <td>{{ $global.getAmountFormat(accountInfo.balanceNQT) }}</td>
            </tr>
            <tr>
              <th>
                {{ $t("account_info.account_available_balance") }}
                <el-tooltip
                  class="item"
                  effect="dark"
                  :content="$t('account_info.account_name_not_set_explain')"
                  placement="top-start"
                >
                  <p class="el-icon-info"></p>
                </el-tooltip>
              </th>
              <td>
                {{ $global.getAmountFormat(accountInfo.effectiveBalanceNQT) }}
              </td>
            </tr>
            <tr>
              <th>{{ $t("account_info.frozen_balance_nqt") }}</th>
              <td>
                {{ $global.getAmountFormat(accountInfo.frozenBalanceNQT) }}
              </td>
            </tr>
            <tr>
              <th>
                {{ $t("account_info.account_mining_balance") }}
                <el-tooltip
                  class="item"
                  effect="dark"
                  :content="$t('account_info.account_reward_balance_explain')"
                  placement="top-start"
                >
                  <p class="el-icon-info"></p>
                </el-tooltip>
              </th>
              <td>
                {{ $global.getAmountFormat(accountInfo.forgedBalanceNQT) }}
              </td>
            </tr>
            <tr>
              <th>{{ $t("network.poc_score") }}</th>
              <td>{{ accountInfo.pocScore }}</td>
            </tr>
            <tr>
              <th>{{ $t("account_info.public_key") }}</th>
              <td>{{ accountInfo.publicKey }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pocScore-detail">
        <el-table
          :data="pocScoreDetail"
          style="width: 100%"
          :show-header="false"
        >
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form
                label-position="left"
                inline
                class="pocScore-table-expand"
              >
                <div>
                  <el-row :gutter="15">
                    <el-col :span="8">
                      <el-form-item :label="$t('network.poc_score_ss')">
                        <span>{{ props.row.ssScore }}</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item :label="$t('network.poc_score_node_type')">
                        <span>{{ props.row.nodeTypeScore }}</span>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="15">
                    <el-col :span="8">
                      <el-form-item :label="$t('network.poc_score_hardware')">
                        <span>{{ props.row.hardwareScore }}</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item :label="$t('network.poc_score_network')">
                        <span>{{ props.row.networkScore }}</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item
                        :label="$t('network.poc_score_performance')"
                      >
                        <span>{{ props.row.performanceScore }}</span>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>
              </el-form>
            </template>
          </el-table-column>
          <el-table-column>
            <template slot-scope="props">
              <span class="primary-color-address">{{ props.row.address }}</span>
            </template>
          </el-table-column>
          <el-table-column>
            <template slot-scope="props">
              <span>{{ props.row.nodeType }}</span>
            </template>
          </el-table-column>
          <el-table-column>
            <template slot-scope="props">
              <span>{{ props.row.pocScoreTotal }}</span>
            </template>
          </el-table-column>
          <el-table-column>
            <template slot-scope="props">
              <span>{{ props.row.nodeTime }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- AssetsAcrossChainsDialog -->
    <div class="modal" id="assets_across_chains_modal" v-show="AssetsAcrossChainsDialog" v-bind:class="{ 'modal-hidden': showChain }"
    >
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">{{ $t("acrossChains.title") }}</h4>
          </div>
          <div class="modal-body modal-message">
            <ul class="title" v-if="!showChain">
              <li :class="chainId === 1 ? 'active' : ''" @click="showChains(1)">
                <a>Heco</a>
              </li>
              <li :class="chainId === 2 ? 'active' : ''" @click="showChains(2)">
                <a>OKEx</a>
              </li>
              <li :class="chainId === 3 ? 'active' : ''" @click="showChains(3)">
                <a>ETH</a>
              </li>
              <!-- <li :class="chainId === 4 ? 'active' : ''" @click="showChains(4)">
		                <a>Tron</a>
		              </li> -->
              <li :class="chainId === 5 ? 'active' : ''" @click="showChains(5)">
                <a>BSC</a>
              </li>
              <li :class="chainId === 6 ? 'active' : ''" @click="showChains(6)">
                <a>...</a>
              </li>
            </ul>
            <ul class="title" v-else>
              <li :class="chainId === 1 ? 'active' : ''"><a>Heco</a></li>
              <li :class="chainId === 2 ? 'active' : ''"><a>OKEx</a></li>
              <li :class="chainId === 3 ? 'active' : ''"><a>ETH</a></li>
              <!-- <li :class="chainId === 4 ? 'active' : ''"><a>Tron</a></li> -->
              <li :class="chainId === 5 ? 'active' : ''"><a>BSC</a></li>
              <li :class="chainId === 4 ? 'active' : ''"><a>...</a></li>
            </ul>

            <div id="content">
              <!--Heco-->
              <el-form class="mod" v-if="chainId === 1">
                <el-form-item
                  :label="$t('acrossChains.Heco_target_address')"
                  class="item_address"
                >
                  <el-input
                    v-model="acrossChains.Heco.target_address"
                    :placeholder="$t('acrossChains.Heco_address_tip')"
                  >
                    <el-button
                      slot="append"
                      style="background: #3fb09a; color: #fff"
                      @click="bindAddress(1)"
                      :disabled="showChain"
                    >
                      {{ $t("acrossChains.bind") }}
                    </el-button>
                  </el-input>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Heco_target_balance')"
                  class="item_balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.Heco.target_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.HECO }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Heco_convertible_balance')"
                  class="balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.Heco.convertible_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.HECO }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Heco_rate')"
                  class="rate"
                  style="margin-bottom: 0"
                >
                </el-form-item>
                <div style="width: 100%; height: 40px">
                  <input
                    class="input__mw"
                    v-model="acrossChains.Heco.rateMW"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_unit">{{ $global.unit }}</label>
                  <label class="input__equals">=</label>
                  <input
                    class="input__heco"
                    v-model="acrossChains.Heco.rateHeco"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_hecoUnit">{{ $global.acrossUnit.HECO }}</label>
                  <el-button
                    class="input_exchange"
                    slot="append"
                    @click="openExchangeDialog"
                    :disabled="showChain"
                    style="background: #3fb09a; color: #fff"
                  >
                    {{ $t("acrossChains.exchange") }}
                  </el-button>
                </div>
              </el-form>
              <!--OKEx-->
              <el-form class="mod" v-if="chainId === 2">
                <el-form-item
                  :label="$t('acrossChains.OKEx_target_address')"
                  class="item_address"
                >
                  <el-input
                    v-model="acrossChains.OKEx.target_address"
                    :placeholder="$t('acrossChains.OKEx_address_tip')"
                  >
                    <el-button
                      slot="append"
                      style="background: #3fb09a; color: #fff"
                      @click="bindAddress(2)"
                      :disabled="showChain"
                    >
                      {{ $t("acrossChains.bind") }}
                    </el-button>
                  </el-input>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.OKEx_target_balance')"
                  class="item_balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.OKEx.target_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.OKEX }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.OKEx_convertible_balance')"
                  class="balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.OKEx.convertible_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.OKEX }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.OKEx_rate')"
                  class="rate"
                  style="margin-bottom: 0"
                >
                </el-form-item>
                <div style="width: 100%; height: 40px">
                  <input
                    class="input__mw"
                    v-model="acrossChains.OKEx.rateMW"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_unit">{{ $global.unit }}</label>
                  <label class="input__equals">=</label>
                  <input
                    class="input__heco"
                    v-model="acrossChains.OKEx.rateOKEx"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_hecoUnit">{{ $global.acrossUnit.OKEX }}</label>
                  <el-button
                    class="input_exchange"
                    slot="append"
                    @click="openExchangeDialog"
                    :disabled="showChain"
                    style="background: #3fb09a; color: #fff"
                  >
                    {{ $t("acrossChains.exchange") }}
                  </el-button>
                </div>
              </el-form>
              <!--ETH-->
              <el-form class="mod" v-if="chainId === 3">
                <el-form-item
                  :label="$t('acrossChains.ETH_target_address')"
                  class="item_address"
                >
                  <el-input
                    v-model="acrossChains.ETH.target_address"
                    :placeholder="$t('acrossChains.ETH_address_tip')"
                  >
                    <el-button
                      slot="append"
                      style="background: #3fb09a; color: #fff"
                      @click="bindAddress(3)"
                      :disabled="showChain"
                    >
                      {{ $t("acrossChains.bind") }}
                    </el-button>
                  </el-input>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.ETH_target_balance')"
                  class="item_balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.ETH.target_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.ETH }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.ETH_convertible_balance')"
                  class="balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.ETH.convertible_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.ETH }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.ETH_rate')"
                  class="rate"
                  style="margin-bottom: 0"
                >
                </el-form-item>
                <div style="width: 100%; height: 40px">
                  <input
                    class="input__mw"
                    v-model="acrossChains.ETH.rateMW"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_unit">{{ $global.unit }}</label>
                  <label class="input__equals">=</label>
                  <input
                    class="input__heco"
                    v-model="acrossChains.ETH.rateETH"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_hecoUnit">{{ $global.acrossUnit.ETH }}</label>
                  <el-button
                    class="input_exchange"
                    slot="append"
                    @click="openExchangeDialog"
                    :disabled="showChain"
                    style="background: #3fb09a; color: #fff"
                  >
                    {{ $t("acrossChains.exchange") }}
                  </el-button>
                </div>
              </el-form>
              <!--Tron-->
              <el-form class="mod" v-if="chainId === 4">
                <el-form-item
                  :label="$t('acrossChains.Tron_target_address')"
                  class="item_address"
                >
                  <el-input
                    v-model="acrossChains.Tron.target_address"
                    :placeholder="$t('acrossChains.Tron_address_tip')"
                  >
                    <el-button
                      slot="append"
                      style="background: #3fb09a; color: #fff"
                      @click="bindAddress(4)"
                      :disabled="showChain"
                    >
                      {{ $t("acrossChains.bind") }}
                    </el-button>
                  </el-input>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Tron_target_balance')"
                  class="item_balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.Tron.target_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.Tron }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Tron_convertible_balance')"
                  class="balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.Tron.convertible_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.TRON }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.Tron_rate')"
                  class="rate"
                  style="margin-bottom: 0"
                >
                </el-form-item>
                <div style="width: 100%; height: 40px">
                  <input
                    class="input__mw"
                    v-model="acrossChains.Tron.rateMW"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_unit">{{ $global.unit }}</label>
                  <label class="input__equals">=</label>
                  <input
                    class="input__heco"
                    v-model="acrossChains.Tron.rateTron"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_hecoUnit">{{ $global.acrossUnit.TRON }}</label>
                  <el-button
                    class="input_exchange"
                    slot="append"
                    @click="openExchangeDialog"
                    :disabled="showChain"
                    style="background: #3fb09a; color: #fff"
                  >
                    {{ $t("acrossChains.exchange") }}
                  </el-button>
                </div>
              </el-form>
              <!--BSC-->
              <el-form class="mod" v-if="chainId === 5">
                <el-form-item
                  :label="$t('acrossChains.BSC_target_address')"
                  class="item_address"
                >
                  <el-input
                    id="acrossChains_target_address"
                    v-model="acrossChains.BSC.target_address"
                    :placeholder="$t('acrossChains.BSC_address_tip')"
                  >
                    <el-button
                      slot="append"
                      style="background: #3fb09a; color: #fff"
                      @click="bindAddress(5)"
                      :disabled="showChain"
                    >
                      {{ $t("acrossChains.bind") }}
                    </el-button>
                  </el-input>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.BSC_target_balance')"
                  class="item_balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.BSC.target_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.BSC }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.BSC_convertible_balance')"
                  class="balance"
                >
                  <input
                    class="el-input__inner"
                    v-model="acrossChains.BSC.convertible_balance"
                    type="number"
                    disabled="disabled"
                  />
                  <label class="input_suffix">{{ $global.acrossUnit.BSC }}</label>
                </el-form-item>
                <el-form-item
                  :label="$t('acrossChains.BSC_rate')"
                  class="rate"
                  style="margin-bottom: 0"
                >
                </el-form-item>
                <div style="width: 100%; height: 40px">
                  <input
                    class="input__mw"
                    v-model="acrossChains.BSC.rateMW"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_unit">{{ $global.unit }}</label>
                  <label class="input__equals">=</label>
                  <input
                    class="input__heco"
                    v-model="acrossChains.BSC.rateBSC"
                    type="text"
                    disabled="disabled"
                  />
                  <label class="input_hecoUnit">{{ $global.acrossUnit.BSC }}</label>
                  <el-button
                    class="input_exchange"
                    slot="append"
                    @click="openExchangeDialog"
                    :disabled="showChain"
                    style="background: #3fb09a; color: #fff"
                  >
                    {{ $t("acrossChains.exchange") }}
                  </el-button>
                </div>
              </el-form>
              <el-form class="mod" v-if="chainId === 6">
                {{ $t("acrossChains.more") }}
              </el-form>
            </div>
          </div>
          <div class="modal-footer">
            {{ $t("acrossChains.tip-0") }}
            <br />

            {{ $t("acrossChains.tip-1") }}
            <br />

            {{ $t("acrossChains.tip-2") }}
            <a>{{
              chainId === 1
                ? acrossChains.Heco.CosExchangeAddress
                : chainId === 2
                ? acrossChains.OKEx.CosExchangeAddress
                : chainId === 3
                ? acrossChains.ETH.CosExchangeAddress
                : chainId === 4
                ? acrossChains.Tron.CosExchangeAddress
                : chainId === 5
                ? acrossChains.BSC.CosExchangeAddress
                : ""
            }}</a>
            {{ $t("acrossChains.tip-3")
            }}{{
              chainId === 1
                ? "Heco"
                : chainId === 2
                ? "OKEx"
                : chainId === 3
                ? "ETH"
                : chainId === 4
                ? "Tron"
                : chainId === 5
                ? "BSC"
                : ""
            }}
            {{ $t("acrossChains.tip-4")
            }}{{
              chainId === 1
                ? "Heco"
                : chainId === 2
                ? "OKEx"
                : chainId === 3
                ? "ETH"
                : chainId === 4
                ? "Tron"
                : chainId === 5
                ? "BSC"
                : ""
            }}
            {{ $t("acrossChains.tip-5")
            }}{{
              chainId === 1
                ? "Heco"
                : chainId === 2
                ? "OKEx"
                : chainId === 3
                ? "ETH"
                : chainId === 4
                ? "Tron"
                : chainId === 5
                ? "BSC"
                : ""
            }}
            {{ $t("acrossChains.tip-6") }}
            <a>{{
              chainId === 1
                ? acrossChains.Heco.ExchangeAddress
                : chainId === 2
                ? acrossChains.OKEx.ExchangeAddress
                : chainId === 3
                ? acrossChains.ETH.ExchangeAddress
                : chainId === 4
                ? acrossChains.Tron.ExchangeAddress
                : chainId === 5
                ? acrossChains.BSC.ExchangeAddress
                : ""
            }}</a>
            {{ $t("acrossChains.tip-7") }}
            <br />
            {{ $t("acrossChains.tip-8") }}
            <br />

            {{ $t("acrossChains.tip-9") }}

            <br />
            {{ $t("acrossChains.tip-10") }}
            <a>
              {{
                chainId === 1
                  ? acrossChains.Heco.contractAddress
                  : chainId === 2
                  ? acrossChains.OKEx.contractAddress
                  : chainId === 3
                  ? acrossChains.ETH.contractAddress
                  : chainId === 4
                  ? acrossChains.Tron.contractAddress
                  : chainId === 5
                  ? acrossChains.BSC.contractAddress
                  : ""
              }}
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- view asset exchange dialog -->
    <div class="modal" v-show="AssetsExchangeDialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button class="close" @click="closeDialog"></button>
            <h4 class="modal-title">
              {{chainId === 1
                  ? $t("acrossChains.hmw_exchange_title")
                  : chainId === 2
                  ? $t("acrossChains.omw_exchange_title")
                  : chainId == 3
                  ? $t("acrossChains.emw_exchange_title")
                  : chainId == 4
                  ? $t("acrossChains.tmw_exchange_title")
                  : chainId == 5
                  ? $t("acrossChains.bmw_exchange_title")
                  : ""
              }}
            </h4>
          </div>
          <div class="modal-body modal-message">
            <el-form>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
                v-if="chainId === 1"
              >
                <masked-input
                  mask="AAA-****-****-****-*****"
                  v-model="acrossChains.Heco.CosExchangeAddress"
                  disabled
                />
                <img src="../../assets/img/account_directory.svg" />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
                v-else-if="chainId === 2"
              >
                <masked-input
                  mask="AAA-****-****-****-*****"
                  v-model="acrossChains.OKEx.CosExchangeAddress"
                  disabled
                />
                <img src="../../assets/img/account_directory.svg" />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
                v-else-if="chainId === 3"
              >
                <masked-input
                  mask="AAA-****-****-****-*****"
                  v-model="acrossChains.ETH.CosExchangeAddress"
                  disabled
                />
                <img src="../../assets/img/account_directory.svg" />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
                v-else-if="chainId === 4"
              >
                <masked-input
                  mask="AAA-****-****-****-*****"
                  v-model="acrossChains.Tron.CosExchangeAddress"
                  disabled
                />
                <img src="../../assets/img/account_directory.svg" />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver')"
                class="item_receiver"
                v-else-if="chainId === 5"
              >
                <masked-input
                  mask="AAA-****-****-****-*****"
                  v-model="acrossChains.BSC.CosExchangeAddress"
                  disabled
                />
                <img src="../../assets/img/account_directory.svg" />
              </el-form-item>
              <el-form-item
                :label="$t('transfer.receiver_public_key')"
                v-if="transfer.hasPublicKey"
              >
                <el-input
                  v-model="transfer.publicKey"
                  type="password"
                ></el-input>
              </el-form-item>
              <el-form-item :label="$t('transfer.amount')">
                <el-button class="calculate_fee" @click="setMaxConvertibleBalance()">
                  {{ $t("acrossChains.all_convertible_balance") }}
                </el-button>
                <el-button class="calculate_fee" @click="setMediumConvertibleBalance()">
                  {{ $t("acrossChains.medium_convertible_balance") }}
                </el-button>
                <input class="el-input__inner" v-model="transfer.exchangeNumber" type="number"/>
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
              <el-form-item :label="$t('transfer.fee')">
                <input class="el-input__inner" v-model="transfer.fee" type="number"/>
                <label class="input_suffix">{{ $global.unit }}</label>
              </el-form-item>
<!--              <el-form-item label="">-->
<!--                <el-checkbox v-model="transfer.hasMessage" disabled>-->
<!--                    {{ $t("transfer.enable_add_info") }}-->
<!--                </el-checkbox>-->
<!--                <el-checkbox ref="encrypted2" v-model="transfer.isEncrypted" :disabled="!transfer.hasMessage">-->
<!--                    {{ $t("transfer.encrypted_information") }}-->
<!--                </el-checkbox>-->
<!--                <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 10 }" resize="none" :placeholder="$t('transfer.message_tip')" v-model="transfer.message" :disabled="!transfer.hasMessage">-->
<!--                </el-input>-->
<!--              </el-form-item>-->
<!--              <el-form-item :label="$t('transfer.secret_key')" v-if="!secretPhrase">-->
<!--                <el-input v-model="transfer.password" type="password"></el-input>-->
<!--              </el-form-item>-->
            </el-form>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              v-loading="transfer.executing"
              class="btn common_btn writeBtn"
              @click="sendExchangeTransferInfo"
              :disabled="isDisable"
            >
              {{ $t("transfer.transfer_send") }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal" id="showChain" v-show="showChain">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="content address">
            {{
              chainId === 1
                ? $t("acrossChains.tipChian1")
                : chainId === 2
                ? $t("acrossChains.tipChian2")
                : chainId === 3
                ? $t("acrossChains.tipChian3")
                : chainId === 4
                ? $t("acrossChains.tipChian4")
                : chainId === 5
                ? $t("acrossChains.tipChian5")
                : ""
            }}
            <a>{{
              chainId === 1
                ? acrossChains.Heco.target_address
                : chainId === 2
                ? acrossChains.OKEx.target_address
                : chainId === 3
                ? acrossChains.ETH.target_address
                : chainId === 4
                ? acrossChains.Tron.target_address
                : chainId === 5
                ? acrossChains.BSC.target_address
                : ""
            }}</a>
          </div>
        </div>
        <div class="modal-footer">
          <el-button
            class="sureButton"
            slot="append"
            style="color: #000"
            @click="bindChainAddress()"
          >
            {{ $t("acrossChains.sure") }}
          </el-button>
          <el-button
            class="cancelButton"
            slot="append"
            style="color: #000"
            @click="cancel()"
          >
            {{ $t("acrossChains.cancel") }}
          </el-button>
        </div>
      </div>
    </div>

    <dialogCommon
      :tradingInfoOpen="tradingInfoDialog"
      :trading="trading"
      :accountInfoOpen="accountInfoDialog"
      :generatorRS="generatorRS"
      :blockInfoOpen="blockInfoDialog"
      :height="height"
      @isClose="isClose"
    ></dialogCommon>
    <AdminPwd
      :openDialog="adminPasswordDialog"
      @getPwd="getAdminPassword"
      @isClose="isClose"
    ></AdminPwd>
    <SecretPhrase
      :openDialog="secretPhraseDialog"
      @getPwd="getSecretPhrase"
      @isClose="isClose"
    ></SecretPhrase>
  </div>
</template>
<script>
import rules from "../../utils/rules";
import { FoundationApiUrls, getCommonFoundationApiUrl } from "../../utils/apiUrl";
import { BigNumber } from "bignumber.js";

export default {
  name: "Network",
  data() {
    let required = rules.required(this.$t('rules.mustRequired'));
    let validateSSAddress = rules.ssAddress(
      this.$t('notification.hubsetting_account_address_error_format'),
      this.$t('rules.mustRequired')
    );
    return {
      projectName: this.$global.projectName,
      nonePublicKeyHint: false,
      isDisable: false,
      isMobile: false,
      //dialog
      src: "",
      requestUrl: "https://sharder.org",
      sendSuccess: false, //true验证码发送 false验证码未发送
      time: 60, //时间
      AssetsAcrossChainsDialog: false,
      AssetsExchangeDialog: false,
      sendMessageDialog: false,
      storageFileDialog: false,
      onChainDialog: false,
      joinNetDialog: false,
      tranferAccountsDialog: false,
      batchTranferAccountsDialog: false,
      hubSettingDialog: false,
      hubInitDialog: false,
      useNATServiceDialog: false,
      registerNatLoading: false,
      tradingInfoDialog: false,
      userInfoDialog: false,
      accountInfoDialog: false,
      adminPasswordDialog: false,
      adminPasswordTitle: '',
      secretPhraseDialog: false,
      initHUb: this.$store.state.isHubInit,
      userConfig: {
        nodeType: this.$store.state.userConfig['sharder.NodeType'],
        xxx: this.$store.state.userConfig['sharder.xxx'],
        phase: this.$store.state.userConfig['sharder.phase'],
        diskCapacity: this.$store.state.userConfig['sharder.diskCapacity'],
        useNATService: this.$store.state.userConfig['sharder.useNATService'] === 'true',
        natClientSecretKey: this.$store.state.userConfig['sharder.NATClientKey'],
        publicAddress: this.$store.state.userConfig['sharder.myAddress'],
        natPort: this.$store.state.userConfig['sharder.NATServicePort'],
        natAddress: this.$store.state.userConfig['sharder.NATServiceAddress'],
        ssAddress: this.$store.state.userConfig['sharder.HubBindAddress'],
        siteAccount: this.$store.state.userConfig['sharder.siteAccount'],
        factoryNum: this.$store.state.userConfig['sharder.factoryNum'],
        permissionMode: this.$store.state.userConfig['sharder.permissionMode'],
        permissionModeDisplay: this.$store.state.userConfig['sharder.permissionMode'],
      },
      registerSharderSiteUser: {
        sharderAccountPhoneOrEmail: '',
        verificationCode: "",
        setSharderPwd: '',
        confirmSharderPwd: '',
        pictureVerificationCode: "",
      },
      needRegister: false,
      isShowName: true,
      generatorRS: '',
      secretPhrase: SSO.secretPhrase,
      blockInfoDialog: false,
      height: '',
      publicKey: SSO.publicKey,
      messageForm: {
        errorCode: false,
        receiver: this.$global.receiverPrefixStr,
        message: "",
        executing: false,
        isEncrypted: false,
        hasPublicKey: false,
        isFile: false,
        publicKey: "",
        senderPublickey: SSO.publicKey,
        fileName: "",
        password: "",
        fee: 1
      },
      file: null,
      storagefile: null,
      parsefile: null,
      onchainfile: null,
      transfer: {
        receiver: this.$global.receiverPrefixStr,
        number: 0,
        exchangeNumber: 0,
        fee: 1,
        hasMessage: false,
        message: "",
        isEncrypted: false,
        executing: false,
        password: "",
        hasPublicKey: false,
        publicKey: "",
        errorCode: false
      },
      batch_transfer: {
        airdropSecretKey: '',
        executing: false,
        executingAnother: false,
        fileName: "",
        isFile: false,
        number: 0,
        password: "",
        hasPublicKey: false,
        publicKey: "",
        errorCode: false,
        jsonData: ""
      },
      hubsetting: {
        registerSiteAccount: false,
        openPunchthrough: false,
        loadingData: false,
        sharderPwd: '',
        diskCapacity: -1,
        address: '',
        port: '',
        clientSecretkey: '',
        publicAddress: '',
        SS_Address: '',
        isOpenMining: true,
        modifyMnemonicWord: '',
        newPwd: '',
        confirmPwd: '',
        register_status: '',
        register_status_text: '',
        initialSerialClickCount: 0,
        settingSerialClickCount: 0,
        natSerialClickCount: 0,
        executing: false,
        airdropAccount: '',
        airdropStatus: false,
      },
      unconfirmedTransactionsList: [],
      blockchainState: this.$global.blockchainState,
      accountInfo: {
        account: '',
        accountId: '',
        name: '',
        accountRS: SSO.accountRS,
        balanceNQT: 0,              //账户余额
        effectiveBalanceNQT: 0,      //可用余额
        forgedBalanceNQT: 0,        //挖矿余额
        frozenBalanceNQT: 0,        //冻结余额
        guaranteedBalanceNQT: 0,    //保证余额
        publicKey: SSO.publicKey,
        requestProcessingTime: '',
        unconfirmedBalanceNQT: '',
        pocScore: '--',
      },
      pocScoreDetail: [{
        bcScore: 0,
        blockMissScore: 0,
        effectiveBalance: 1,
        hardwareScore: 0,
        height: 0,
        networkScore: 0,
        nodeTypeScore: 0,
        onlineRateScore: 0,
        performanceScore: 0,
        serverScore: 0,
        ssScore: 0,
        address: SSO.accountRS,
        nodeType: 0,
        pocScoreTotal: 0,
        nodeTime: "--",
      }],
      selectType: '',
      transactionType: [{
        value: '',
        label: this.$t('transaction.transaction_type_all')
      }, {
        value: 0,
        label: this.$t('transaction.transaction_type_payment')
      }, {
        value: 1,
        label: this.$t('transaction.transaction_type_information')
      }, {
        value: 1.5,
        label: this.$t('transaction.transaction_type_account')
      }, {
        value: 11,
        label: this.$t('transaction.transaction_type_storage_service')
      }, {
        value: 8,
        label: this.$t('transaction.transaction_type_forge_pool')
      }, {
        value: 9,
        label: this.$t('transaction.transaction_type_system_reward')
      }, {
        value: 12,
        label: this.$t('transaction.transaction_type_poc')
      }, {
        value: 18,
        label: this.$t('transaction.transaction_type_burn')
      },
      ],
      exchangeSelectType: '',
      exchangeTransactionType: [
        {
          value: "",
          label: this.$t("transaction.transaction_type_exchange"),
        },
        {
          value: 1,
          label: this.$t("transaction.transaction_type_mw_to_hmw"),
        },
        {
          value: 2,
          label: this.$t("transaction.transaction_type_hmw_to_mw"),
        },
        {
          value: 3,
          label: this.$t("transaction.transaction_type_mw_to_omw"),
        },
        {
          value: 4,
          label: this.$t("transaction.transaction_type_omw_to_mw"),
        },
        {
          value: 5,
          label: this.$t("transaction.transaction_type_mw_to_emw"),
        },
        {
          value: 6,
          label: this.$t("transaction.transaction_type_emw_to_mw"),
        },
        // {
        //   value: 7,
        //   label: this.$t("transaction.transaction_type_mw_to_tmw"),
        // },
        // {
        //   value: 8,
        //   label: this.$t("transaction.transaction_type_tmw_to_mw"),
        // },
        {
          value: 9,
          label: this.$t("transaction.transaction_type_mw_to_bmw"),
        },
        {
          value: 10,
          label: this.$t("transaction.transaction_type_bmw_to_mw"),
        },
      ],
      trading: '',
      accountTransactionList: [],
      //分页信息
      currentPage: 1,
      totalSize: 0,
      pageSize: 10,

      latesetVersion: '',
      upgradeMode: '',
      bakMode: '',
      isUpdate: false,
      loading: true,

      capacity: 0,
      maxCapacity: 192,
      diskPledge: 0,
      accountSecret: "",

      params: [],
      temporaryName: '',
      ssPublickey: SSO.publicKey,

      operationType: 'init',
      formRules: {},
      registerSharderSiteUserRules: {
        sharderAccountPhoneOrEmail: [{ required: true, message: this.$t('rules.mustRequired') }],
        verificationCode: [{ required: true, message: this.$t('binding_validation.verification_title') }],
        setSharderPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (value) {
                if (this.registerSharderSiteUser.setSharderPwd) {
                  this.$refs['initForm'].validateField('confirmSharderPwd');
                }
                callback();
              } else {
                callback(new Error(this.$t('rules.plz_input_sharder_pwd')));
              }
            },
            trigger: 'blur'
          }
        ],
        confirmSharderPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!value && this.registerSharderSiteUser.setSharderPwd) {
                callback(new Error(this.$t('rules.plz_input_sharder_pwd_again')));
              } else if (value !== this.registerSharderSiteUser.setSharderPwd) {
                callback(new Error(this.$t('rules.inconsistent_sharder_password')));
              } else {
                callback();
              }
            },
            trigger: 'blur'
          }
        ],
        pictureVerificationCode: [{ required: true, message: this.$t('rules.mustRequired') }],
      },
      hubInitSettingRules: {
        publicAddress: [{ required: true, message: this.$t('rules.mustRequired') }],
        sharderAccount: [{ required: true, message: this.$t('rules.mustRequired') }],
        sharderPwd: [{ required: true, message: this.$t('rules.mustRequired') }],
        modifyMnemonicWord: [{
          required: true,
          // message: this.$t('rules.mustRequired')
          validator: (rule, value, callback) => {
            if (value) {
              if (this.hubsetting.modifyMnemonicWord !== SSO.secretPhrase) {
                callback(new Error(this.$t('notification.hubsetting_not_matched_mnemonic_word')));
              }
            } else {
              callback(new Error(this.$t('rules.mustRequired')));
            }
          },
          trigger: 'blur'
        }],
        address: [{ required: true, message: this.$t('rules.mustRequired') }],
        port: [{ required: true, message: this.$t('rules.mustRequired') }],
        clientSecretkey: [{ required: true, message: this.$t('rules.mustRequired') }],
        newPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (value) {
                if (this.hubsetting.confirmPwd) {
                  this.$refs['initForm'].validateField('confirmPwd');
                }
                callback();
              } else {
                callback(new Error(this.$t('rules.plz_input_admin_pwd')));
              }
            },
            trigger: 'blur'
          }
        ],
        confirmPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!value && this.hubsetting.newPwd) {
                callback(new Error(this.$t('rules.plz_input_admin_pwd_again')));
              } else if (value !== this.hubsetting.newPwd) {
                callback(new Error(this.$t('rules.inconsistent_admin_password')));
              } else {
                callback();
              }
            },
            trigger: 'blur'
          }
        ],
      },
      hubReconfigureSettingRules: {
        sharderAccount: [{ required: true, message: this.$t('rules.mustRequired') }],
        sharderPwd: [{ required: true, message: this.$t('rules.mustRequired') }],
        modifyMnemonicWord: [{
          required: true,
          // message: this.$t('rules.mustRequired')
          validator: (rule, value, callback) => {
            if (value) {
              if (this.hubsetting.modifyMnemonicWord !== SSO.secretPhrase) {
                callback(new Error(this.$t('notification.hubsetting_not_matched_mnemonic_word')));
              }
            } else {
              callback(new Error(this.$t('rules.mustRequired')));
            }
          },
          trigger: 'blur'
        }],
        address: [{ required: true, message: this.$t('rules.mustRequired') }],
        port: [{ required: true, message: this.$t('rules.mustRequired') }],
        clientSecretkey: [{ required: true, message: this.$t('rules.mustRequired') }],
        publicAddress: [{ required: true, message: this.$t('rules.mustRequired') }],
        newPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (value) {
                if (this.hubsetting.confirmPwd) {
                  this.$refs['reconfigureForm'].validateField('confirmPwd');
                }
              }
              callback();
            },
            trigger: 'blur'
          }
        ],
        confirmPwd: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!value && this.hubsetting.newPwd) {
                callback(new Error(this.$t('rules.plz_input_admin_pwd_again')));
              } else if (value !== this.hubsetting.newPwd) {
                callback(new Error(this.$t('rules.inconsistent_admin_password')));
              } else {
                callback();
              }
            },
            trigger: 'blur'
          }
        ],
      },
      resettingRules: {
        sharderPwd: [{ required: true, message: this.$t('rules.mustRequired') }],
        sharderAccount: [{ required: true, message: this.$t('rules.mustRequired') }],
      },
      airdropFlag: false,
      acrossChains: {
        Heco: {
          target_address: '',
          target_balance: 0,
          old_address: '',
          convertible_balance: 0,
          rateMW: 10,
          rateHeco: 1,
          CosExchangeAddress: "SSA-XXXX-XXXX-XXXX-HECO",
          CosExchangeAddressPublicKey: "",
          CosExchangeRate: 10,
          ExchangeAddress: "0x0Heco",
          contractAddress: "0x0000Heco",
        },
        OKEx: {
          target_address: '',
          target_balance: 0,
          old_address: '',
          convertible_balance: 0,
          rateMW: 10,
          rateOKEx: 1,
          CosExchangeAddress: "SSA-XXXX-XXXX-XXXX-OKEX",
          CosExchangeAddressPublicKey: "",
          CosExchangeRate: 10,
          ExchangeAddress: "0x0OKEx",
          contractAddress: "0x0000OKEx",
        },
        ETH: {
          target_address: '',
          target_balance: 0,
          old_address: '',
          convertible_balance: 0,
          rateMW: 10,
          rateETH: 1,
          CosExchangeAddress: "SSA-XXXX-XXXX-XXXX-ETH",
          CosExchangeAddressPublicKey: "",
          CosExchangeRate: 10,
          ExchangeAddress: "0x0ETH",
          contractAddress: "0x0000ETH",
        },
        Tron: {
          target_address: '',
          target_balance: 0,
          old_address: '',
          convertible_balance: 0,
          rateMW: 10,
          rateTron: 1,
          CosExchangeAddress: "SSA-XXXX-XXXX-XXXX-TRON",
          CosExchangeAddressPublicKey: "",
          CosExchangeRate: 10,
          ExchangeAddress: "0x0Tron",
          contractAddress: "0x0000Tron",
        },
        BSC: {
          target_address: '',
          target_balance: 0,
          old_address: '',
          convertible_balance: 0,
          rateMW: 10,
          rateBSC: 1,
          CosExchangeAddress: "SSA-XXXX-XXXX-XXXX-BSC",
          CosExchangeAddressPublicKey: "",
          CosExchangeRate: 10,
          ExchangeAddress: "0x0BSC",
          contractAddress: "0x0000BSC",
        },
        balance: 0,
        id: ''
      },
      chainId: 1,
      showChain: false,
      exchangeOpenButton: true,
    };
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
        this.isMobile = true
      }

      const _this = this;
      let publicKey = null;
      _this.getAccount(_this.accountInfo.accountRS).then(res => {
        _this.accountInfo.account = res.account;
        _this.accountInfo.accountId = res.accountId;
        _this.accountInfo.balanceNQT = res.balanceNQT;
        _this.accountInfo.effectiveBalanceNQT = res.effectiveBalanceNQT;
        _this.accountInfo.forgedBalanceNQT = res.forgedBalanceNQT;
        _this.accountInfo.frozenBalanceNQT = res.frozenBalanceNQT;
        _this.accountInfo.guaranteedBalanceNQT = res.guaranteedBalanceNQT;
        _this.accountInfo.unconfirmedBalanceNQT = res.unconfirmedBalanceNQT;
        if (res.publicKey != null) {
          publicKey = res.publicKey;
        }
        if (res.pocScore != null) {
          _this.accountInfo.pocScore = res.pocScore.total;
          _this.pocScoreDetail[0].bcScore = res.pocScore.bcScore;
          _this.pocScoreDetail[0].blockMissScore = res.pocScore.blockMissScore;
          _this.pocScoreDetail[0].effectiveBalance = res.pocScore.effectiveBalance;
          _this.pocScoreDetail[0].hardwareScore = res.pocScore.hardwareScore;
          _this.pocScoreDetail[0].height = res.pocScore.height;
          _this.pocScoreDetail[0].networkScore = res.pocScore.networkScore;
          _this.pocScoreDetail[0].nodeTypeScore = res.pocScore.nodeTypeScore;
          _this.pocScoreDetail[0].onlineRateScore = res.pocScore.onlineRateScore;
          _this.pocScoreDetail[0].performanceScore = res.pocScore.performanceScore;
          _this.pocScoreDetail[0].serverScore = res.pocScore.serverScore;
          _this.pocScoreDetail[0].ssScore = res.pocScore.ssScore;
          _this.pocScoreDetail[0].nodeType = res.nodeType;
          _this.pocScoreDetail[0].pocScoreTotal = res.pocScore.total;
        }
        _this.accountInfo.name = res.name;
        _this.pocScoreDetail[0].nodeTime = res.declaredTime;
      });
      _this.getAccountTransactionList();
      _this.getDrawData();
      _this.getYieldData();
      _this.$global.setBlockchainState(_this).then(res => {
        _this.blockchainState = res.data;
        _this.getLatestHubVersion();
      }).then(() => {
        _this.nonePublicKeyHint = false;
        if (publicKey == null && !SSO.downloadingBlockchain && _this.blockchainState.blockchainState == "UP_TO_DATE") {
          _this.nonePublicKeyHint = false;
        }
      });

      // SSO.getState();
      _this.$global.getUserConfig(_this).then(res => {
        _this.hubsetting.address = res["sharder.NATServiceAddress"];
        _this.hubsetting.port = res["sharder.NATServicePort"];
        _this.hubsetting.clientSecretkey = res["sharder.NATClientKey"];
        _this.hubsetting.publicAddress = res["sharder.myAddress"];
        _this.hubsetting.airdropAccount = res["sharder.airdrop.accounts"];
        _this.hubsetting.airdropStatus = res["sharder.airdrop.isEnable"];
        //_this.hubsetting.SS_Address = res["sharder.HubBindAddress"];
      });
      // _this.getLatestHubVersion();
      _this.getPicVCode();
      _this.getAcrossAddress();
    },
    // menuAdapter() {
    //     document.getElementsByClassName('header')[0].style.display = 'block'
    //     var menuLi = document.querySelectorAll('.navbar .el-menu li')
    //     for (let i = 0; i < menuLi.length; i++) {
    //         if (i === 0) {
    //             menuLi[i].setAttribute('class', 'el-menu-item is-active')
    //             menuLi[i].style.borderBottomColor = '#409EFF'
    //         } else {
    //             menuLi[i].setAttribute('class', 'el-menu-item')
    //             menuLi[i].style.borderBottomColor = 'transparent'
    //         }
    //     }
    // },
    //定时器
    finish() {
      //禁用以下表单
      const _this = this;
      _this.sendSuccess = true;
      //进行倒计时提示
      let Time = setInterval(() => {
        if (_this.time <= 1) {
          //当时间小于等于一的时候清除定时器
          //初始化一下数据参数
          clearInterval(Time);
          _this.sendSuccess = false;
          _this.time = 60;
        } else {
          _this.time--;
        }
      }, 1000);
    },
    //发送验证码请求
    sendVCode() {
      const _this = this;
      let requestUrl = "";
      let sharderAccount = _this.registerSharderSiteUser.sharderAccountPhoneOrEmail;
      if (sharderAccount === "") {
        _this.$message.warning(_this.$t('notification.sendVCode'));
        return false;
      }
      if (sharderAccount.match(/^([a-z0-9A-Z_]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\.)+[a-zA-Z]{2,}$/)) {
        requestUrl = _this.requestUrl + "/send/email/captcha.ss";
      } else {
        requestUrl = _this.requestUrl + "/send/sms/captcha.ss";
      }
      //调用定时器
      _this.finish();
      //发送请求

      _this.$global.sendVerifyCode(requestUrl, sharderAccount, function (res) {
        if (res.success) {
          _this.$message.warning(_this.$t('mining.binding_validation.verification_tip1') + sharderAccount + "," + _this.$t('mining.binding_validation.verification_tip1'));
        }

      });
    },
    registerSharderSite() {
      const _this = this;
      let sharderAccount = _this.registerSharderSiteUser.sharderAccountPhoneOrEmail;
      let verificationCode = _this.registerSharderSiteUser.verificationCode;
      let setSharderPwd = _this.registerSharderSiteUser.setSharderPwd;
      let confirmSharderPwd = _this.registerSharderSiteUser.confirmSharderPwd;
      let pictureVerificationCode = _this.registerSharderSiteUser.pictureVerificationCode;
      if (sharderAccount === "") {
        _this.$message.warning(_this.$t('rules.plz_input_phone_or_email'));
        return false;
      } else if (verificationCode === "") {
        _this.$message.warning(_this.$t('mining.binding_validation.verification_title'));
        return false;
      } else if (setSharderPwd === "") {
        _this.$message.warning(_this.$t('password_modal.input_sharder_site_pwd'));
        return false;
      } else if (confirmSharderPwd === "") {
        _this.$message.warning(_this.$t('rules.plz_input_sharder_pwd_again'));
        return false;
      } else if (confirmSharderPwd != setSharderPwd) {
        _this.$message.warning(_this.$t('rules.inconsistent_sharder_password'));
        return false;
      } else if (pictureVerificationCode === "") {
        _this.$message.warning(_this.$t('rules.plz_input_pic_code'));
        return false;
      }

      let data = {
        username: sharderAccount,
        email: sharderAccount,
        captcha: verificationCode,
        loginPassword: setSharderPwd,
        imgCaptcha: pictureVerificationCode
      };
      let requestUrl = _this.requestUrl + "/official/register_.ss";
      _this.$global.registerSharderSite(requestUrl, data, function (res) {
        if (res.success) {
          _this.userConfig.siteAccount = sharderAccount;
          _this.hubsetting.sharderPwd = setSharderPwd;
          _this.bindNatService();
          _this.registerSharderSiteUser.sharderAccountPhoneOrEmail = "";
          _this.registerSharderSiteUser.verificationCode = "";
          _this.registerSharderSiteUser.setSharderPwd = "";
          _this.registerSharderSiteUser.confirmSharderPwd = "";
          _this.registerSharderSiteUser.pictureVerificationCode = "";

        } else {
          console.log("jie:" + res.result.data);
          if (res.result.data[0] === "短信验证码错误") {
            _this.$message.error(_this.$t('hubsetting.register_error_tip2'));
          } else if (res.result.data[0] === "图片验证码错误") {
            _this.$message.error(_this.$t('hubsetting.register_error_tip3'));
          } else if (res.result.data[0] === "验证码无效") {
            _this.$message.error(_this.$t('hubsetting.register_error_tip1'));
          } else {
            _this.$message.error(_this.$t('hubsetting.register_error'));
          }

        }
      });

    },
    getPicVCode() {
      const _this = this;
      _this.src = _this.requestUrl + "/captcha.svl?_" + Math.random();
    },
    activeSelectType(type) {
      return this.selectType === type ? 'active' : ''
    },
    getLatestHubVersion() {
      const _this = this;
      _this.$http.get(_this.$global.urlPrefix() + '?requestType=getLatestCosVersion').then(res => {
        if (res.data.success) {
          _this.latesetVersion = res.data.cosver.version;
          _this.upgradeMode = res.data.cosver.mode;
          _this.bakMode = res.data.cosver.bakMode;
          let bool = _this.versionCompare(_this.blockchainState.version, _this.latesetVersion);
          _this.isUpdate = bool;
        } else {
          _this.$message.error(res.data.error ? res.data.error : res.data.errorDescription);
        }
      }).catch(err => {
        // _this.$message.error(err.message);
      });
    },
    drawBarChart: function (barchat) {
      var dom = document.getElementById("transaction_amount_bar");
      if (!dom) {
        console.log("dom transaction_amount_bar got faild，echarts can not load")
        return
      }
      const _this = this;
      const barchart = _this.$echarts.init(dom, null, { renderer: 'svg' });
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
          data: barchat.xAxis,
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          data: barchat.series,
          type: 'bar',
          color: _this.$global.primaryColor,
        }]
      };
      if (option && typeof option === "object") {
        barchart.setOption(option, true);
      }
    },
    drawYield: function (yields) {
      var dom = document.getElementById("yield_curve")
      if (!dom) {
        console.log("dom yield_curve got faild, echarts can not load")
        return
      }
      const _this = this;
      const yieldCurve = _this.$echarts.init(dom, null, { renderer: 'svg' });
      const option = {
        grid: {
          left: '15%',
          right: '2%',
          top: '10%',
          bottom: '30%',
        },
        tooltip: {
          trigger: 'axis'
        },
        dataZoom: [{
          type: 'inside',
          show: false,
          start: 80,
          end: 100
        }, {
          start: 0,
          end: 10,
          handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
          handleSize: '40%',
          handleStyle: {
            color: '#fff',
            shadowBlur: 3,
            shadowColor: 'rgba(0, 0, 0, 0.6)',
            shadowOffsetX: 2,
            shadowOffsetY: 2
          }
        }],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: yields.xAxis,
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: function (value) {
              if (value > 10000000) {
                return value / 10000000 + 'M'
              } else if (value > 1000) {
                return value / 1000 + 'K'
              }
            }
          }
        },
        series: [{
          data: yields.series,
          type: 'line',
          color: _this.$global.primaryColor,
          smooth: true
        }]
      };
      if (option && typeof option === "object") {
        yieldCurve.setOption(option, true);
      }
    },
    handleSizeChange(val) {
      const _this = this;
      _this.getAccountTransactionList();
    },
    handleCurrentChange(val) {
      const _this = this;
      _this.getAccountTransactionList();
    },
    displaySerialNo: function (clickType) {
      const _this = this;
      if ('initial' == clickType) {
        _this.hubsetting.initialSerialClickCount++;
      } else if ('setting' == clickType) {
        _this.hubsetting.settingSerialClickCount++;
      } else if ('nat' == clickType) {
        _this.hubsetting.natSerialClickCount++;
      }
      console.log("clickType=" + clickType
        + ",initialSerialClickCount=" + _this.hubsetting.initialSerialClickCount
        + ",settingSerialClickCount=" + _this.hubsetting.settingSerialClickCount
        + ",natSerialClickCount=" + _this.hubsetting.natSerialClickCount
      )
    },
    updateHubVersion(adminPwd) {
      const _this = this;
      let data = new FormData();
      data.append("version", _this.latesetVersion);
      data.append("mode", _this.upgradeMode);
      data.append("bakMode", _this.bakMode);
      data.append("restart", "true");
      data.append("adminPassword", adminPwd);
      this.$http.post('/sharder?requestType=upgradeClient', data).then(res => {
        if (res.data.upgraded) {
          _this.$message.success(_this.$t('notification.update_success'));
          _this.$store.state.mask = false;
          _this.$router.push("/login");
          //window.location.href = "/";
          _this.autoRefresh();
        } else {
          _this.$message.error(res.data.error ? res.data.error : res.data.errorDescription);
        }
      }).catch(err => {
        // _this.$message.error(err.message);
      });
    },
    restartHub(adminPwd) {
      const _this = this;
      let data = new FormData();
      data.append("adminPassword", adminPwd);
      this.$http.post('/sharder?requestType=restart', data).then(res => {
        if (!res.data.errorDescription) {
          _this.$message.success(_this.$t('restart.restarting'));
          _this.$store.state.mask = false;
          _this.$router.push("/login");
          _this.autoRefresh();
        } else {
          _this.$message.error(res.data.errorDescription);
          _this.closeDialog();
        }
      }).catch(err => {
        // _this.$message.error(err.message);
      });
    },
    resetHub(adminPwd, type) {
      const _this = this;
      let resetData = new FormData();
      resetData.append("adminPassword", adminPwd);
      resetData.append("restart", "true");
      resetData.append("type", type);
      this.$http.post('/sharder?requestType=recovery', resetData).then(res => {
        if (res.data.done) {
          _this.$message.success(_this.$t('restart.restarting'));
          _this.$store.state.mask = false;
          _this.$router.push("/login");
          _this.autoRefresh();
        } else {
          _this.$message.error(res.data.errorDescription ? res.data.errorDescription : res.data.failedReason);
          _this.closeDialog();
        }
      })
    },
    updateHubSetting(params) {
      const _this = this;
      this.$http.post('/sharder?requestType=reConfig', params).then(res => {
        if (res.data.reconfiged) {
          _this.$message.success(_this.$t('restart.restarting'));
          _this.$store.state.mask = false;
          _this.$router.push("/login");
          _this.autoRefresh();
        } else {
          _this.$message.error(res.data.errorDescription ? res.data.errorDescription : res.data.failedReason);
          this.$refs["reconfigureForm"].clearValidate();
          this.$refs["reconfigureForm"].resetFields();
          _this.closeDialog();
        }
      }).catch(err => {
        // _this.$message.error(err.message);
      });
    },
    verifyAndGenerateHubSettingFormData() {
      const _this = this;
      let formData = new FormData();
      // user input datas
      formData.append("restart", true);
      formData.append("sharder.disableAdminPassword", "false");
      formData.append('sharderAccount', _this.userConfig.siteAccount);
      formData.append('password', _this.hubsetting.sharderPwd);
      formData.append('registerStatus', _this.hubsetting.register_status);
      formData.append('nodeType', _this.userConfig.nodeType);
      formData.append("factoryNum", this.userConfig.factoryNum);
      formData.append("permissionMode", this.userConfig.permissionMode);

      // nat settings
      if (_this.hubsetting.openPunchthrough) {
        formData.append("sharder.useNATService", "true");
        if (_this.hubsetting.address === '' ||
          _this.hubsetting.port === '' ||
          _this.hubsetting.clientSecretkey === '') {
          if (_this.hubsetting.sharderPwd === '')
            _this.$message.error(_this.$t('notification.hubsetting_no_sharder_account'));
          else
            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
          return false;
        } else {
          formData.append("sharder.NATServiceAddress", _this.hubsetting.address);
          formData.append("sharder.NATServicePort", _this.hubsetting.port);
          formData.append("sharder.NATClientKey", _this.hubsetting.clientSecretkey);
          formData.append("sharder.myAddress", _this.hubsetting.publicAddress);
        }
      } else {
        formData.append("sharder.NATServiceAddress", "");
        formData.append("sharder.NATServicePort", "");
        formData.append("sharder.NATClientKey", "");
        formData.append("sharder.myAddress", _this.hubsetting.publicAddress);
        formData.append("sharder.useNATService", "false");
      }

      if (_this.hubsetting.SS_Address !== '') {
        if (!_this.hubsetting.SS_Address.toUpperCase().match(_this.$global.pattern)) {
          _this.$message.warning(_this.$t('notification.hubsetting_account_address_error_format'));
          return false;
        } else {
          formData.append("sharder.HubBindAddress", _this.hubsetting.SS_Address);
          formData.append("reBind", true);
        }
      } else {
        formData.append("reBind", false);
      }

      if (_this.hubsetting.isOpenMining) {
        formData.append("sharder.HubBind", true);
        if (_this.hubsetting.modifyMnemonicWord === '') {
          _this.$message.warning(_this.$t('notification.hubsetting_no_mnemonic_word'));
          return false;
        }
        if (SSO.secretPhrase === '') {
          _this.$message.warning(_this.$t('notification.hubsetting_login_again'));
          return false;
        }

        if (_this.hubsetting.modifyMnemonicWord !== SSO.secretPhrase) {
          _this.$message.warning(_this.$t('notification.hubsetting_not_matched_mnemonic_word'));
          return false;
        }
        formData.append("sharder.HubBindPassPhrase", _this.hubsetting.modifyMnemonicWord);
      } else {
        formData.append("sharder.HubBind", false);
      }

      if (_this.hubsetting.newPwd !== '' || _this.hubsetting.confirmPwd !== '') {
        if (_this.hubsetting.newPwd !== _this.hubsetting.confirmPwd) {
          _this.$message.warning(_this.$t('notification.hubsetting_inconsistent_password'));
          return false;
        } else {
          formData.append("newAdminPassword", _this.hubsetting.newPwd);
        }
      }
      if (_this.userConfig.permissionMode && _this.userConfig.factoryNum == null && _this.operationType == 'init') {
        _this.$message.warning(_this.$t('notification.hubsetting_factory_null'));
        return false;
      }
      return formData;
    },
    verifyHubSetting: function (type) {
      this.hubsetting.executing = true;
      const _this = this;
      _this.preventRepeatedClick();
      let reConfigFormData = _this.verifyAndGenerateHubSettingFormData();
      if (reConfigFormData !== false) {
        reConfigFormData.append("isInit", "true");
      }
      if (type === 'init') {
        this.operationType = 'init';
        _this.$refs['initForm'].validate((valid) => {
          if (valid) {
            _this.reconfigure(reConfigFormData);
          } else {
            console.log('init dialog error submit!!');
            return false;
          }
        });
      } else if (type === 'register') {
        if (this.whetherShowConfigureNATServiceBtn()) {
          this.operationType = 'reConfigNormal';
        } else {
          this.operationType = 'initNormal';
        }
        _this.$refs['useNATForm'].validate((valid) => {
          if (valid) {
            _this.reconfigure(reConfigFormData);
          } else {
            console.log('register dialog error submit!!');
            return false;
          }
        });
      }
    },
    registerNatService() {
      console.log("registering nat service for normal node...");
      const _this = this;
      _this.registerNatLoading = true;

      // linked ss address > logged ss address
      let ssAddr = this.userConfig.ssAddress;
      if (ssAddr == undefined || ssAddr == '') {
        ssAddr = _this.getAccountRsBySecret();
      }

      let data = new FormData();
      data.append("sharderAccount", this.userConfig.siteAccount);
      data.append("tssAddress", ssAddr);
      data.append("nodeType", this.userConfig.nodeType);
      data.append("registerStatus", "0");
      this.$http.post(getCommonFoundationApiUrl(FoundationApiUrls.natRegister), data)
        .then(response => {
          _this.registerNatLoading = false;
          if (response.data.success) {
            console.info('success to register NAT service');
            _this.$message.success(_this.$t('notification.success_to_register_nat'));
            _this.closeDialog();
          } else {
            _this.$message.error(`errorCode:${response.data.code}, reason:${response.data.msg}`);
            _this.closeDialog();
            this.$refs["reconfigureForm"].clearValidate();
            this.$refs["reconfigureForm"].resetFields();
          }
        })
        .catch(err => {
          _this.registerNatLoading = false;
        });
    },
    bindNatService() {
      const _this = this;
      let ssAddr = this.userConfig.ssAddress;
      if (ssAddr === undefined || ssAddr === '') {
        ssAddr = _this.getAccountRsBySecret();
      }
      let data = new FormData();
      data.append("sharderAccount", this.userConfig.siteAccount);
      data.append("tssAddress", ssAddr);
      data.append("nodeType", this.userConfig.nodeType);
      data.append("registerStatus", "0");
      this.$http.post(getCommonFoundationApiUrl(FoundationApiUrls.natReservedBinding), data)
        .then(response => {
          if (response.data.success) {
            console.info('success to bind NAT service');
            _this.$message.success(_this.$t('notification.success_to_bind_nat'));
            _this.closeDialog();
          } else {
            _this.$message.error(_this.$t('binding_reservation_penetration_service'));
            _this.closeDialog();
            this.$refs["reconfigureForm"].clearValidate();
            this.$refs["reconfigureForm"].resetFields();
          }
        })
        .catch(err => {
          _this.registerNatLoading = false;
        });

    },
    autoRefresh() {
      setTimeout(() => {
        window.location.reload();
      }, 40000);
    },
    // hubSettingConfirmThenGoAdmin(type, formName) {
    //     let _this = this;
    //     _this.$refs[formName].validate((valid) => {
    //         return valid;
    //     });
    // },
    reconfigure(data) {
      let _this = this;
      this.$http.post('/sharder?requestType=reConfig', data).then(res1 => {
        if (res1.data.reconfiged) {
          console.log('success to reconfigure settings...');
          _this.$message.success(_this.$t('restart.restarting'));
          data = new FormData();
          //window.location = "/";
          _this.$store.state.mask = false;
          _this.$router.push("/login");
          _this.autoRefresh();
        } else {
          let msg = res1.data.errorDescription ? res1.data.errorDescription :
            (res1.data.failedReason ? res1.data.failedReason : 'error');
          _this.$message.error(msg);
          _this.closeDialog();
          console.log('failed to reconfigure settings...');
        }
      }).catch(err => {
        // _this.$message.error(err.message);
      });
    },
    checkPicVerificationCode() {
    },
    checkSiteAccount() {
      const _this = this;
      let formData = new FormData();
      if (_this.userConfig.siteAccount !== ''
        && _this.hubsetting.sharderPwd !== ''
        && _this.hubsetting.openPunchthrough) {

        _this.hubsetting.loadingData = true;

        formData.append("sharderAccount", _this.userConfig.siteAccount);
        formData.append("password", _this.hubsetting.sharderPwd);
        formData.append("serialNum", _this.userConfig.xxx);
        formData.append("nodeType", _this.userConfig.nodeType);
        _this.$http.post(getCommonFoundationApiUrl(FoundationApiUrls.fetchNatServiceConfig), formData)
          .then(res => {
            _this.hubsetting.loadingData = false;
            if (res.data.success && res.data.data) {
              _this.hubsetting.address = res.data.data.natServiceIp;
              _this.hubsetting.port = res.data.data.natServicePort;
              _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
              _this.hubsetting.publicAddress = res.data.data.proxyAddress;
              //_this.hubsetting.SS_Address = res.data.data.tssAddress;
              _this.hubsetting.register_status_text = this.formatRegisterStatus(res.data.data.status);
              _this.hubsetting.register_status = res.data.data.status;
              _this.needRegister = false;
            } else if (res.data.success && !res.data.data) {
              _this.clearHubSetting();
              _this.needRegister = true;
              _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
            } else {
              _this.clearHubSetting();
              _this.$message.error(res.data.errorMessage ? res.data.errorMessage : res.data.message);
            }
          })
          .catch(err => {
            _this.hubsetting.loadingData = false;
          });
      }
    },
    getAccount(account) {
      const _this = this;
      return new Promise((resolve, reject) => {
        this.$http.get(_this.$global.urlPrefix() + '?requestType=getAccount', {
          params: {
            account: account,
            includeLessors: true,
            includeAssets: true,
            includeEffectiveBalance: true,
            includeCurrencies: true,
          }
        }).then(function (res) {
          resolve(res.data);
        }).catch(function (err) {
          console.log(err);
        });
      });
    },
    getMessageFee: function () {
      const _this = this;
      let options = {};
      let encrypted = {};
      let formData = new FormData();
      _this.getAccount(SSO.account).then(res => {
        if (res.errorDescription === "Unknown account") {
          _this.$message.warning(_this.$t('notification.new_account_warning'));
          return;
        }
      });
      if (_this.messageForm.receiver === this.$global.receiverPrefixStr ||
        _this.messageForm.receiver === this.$global.receiverEmptyStr) {
        formData.append("recipient", "");
      } else {
        formData.append("recipient", _this.messageForm.receiver);
        formData.append("recipientPublicKey", _this.messageForm.publicKey);
      }
      formData.append("phased", 'false');
      formData.append("phasingLinkedFullHash", '');
      formData.append("phasingHashedSecret", '');
      formData.append("phasingHashedSecretAlgorithm", '2');
      formData.append("calculateFee", 'true');
      formData.append("broadcast", 'false');
      formData.append("feeNQT", '0');
      formData.append("publicKey", SSO.publicKey);
      formData.append("deadline", '1440');
      if (_this.messageForm.isEncrypted) {
        if (_this.messageForm.receiver === this.$global.receiverPrefixStr ||
          _this.messageForm.receiver === this.$global.receiverEmptyStr) {
          _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
          return;
        }
        if (_this.messageForm.publicKey === "") {
          _this.$message.warning(_this.$t('notification.sendmessage_null_account_public'));
          return;
        }
        if (!(_this.messageForm.password || _this.secretPhrase)) {
          _this.$message.warning(_this.$t('notification.sendmessage_null_secret_key'));
          return;
        }
        options.account = _this.messageForm.receiver;
        options.publicKey = _this.messageForm.publicKey;
        if (_this.messageForm.isFile) {
          formData.append("messageToEncryptIsText", 'false');
          formData.append("encryptedMessageIsPrunable", 'true');
          let encryptionkeys = SSO.getEncryptionKeys(options, _this.messageForm.password || _this.secretPhrase);

          _this.encryptFileCallback(_this.file, encryptionkeys).then(res => {
            formData.append("encryptedMessageFile", res.file);
            formData.append("encryptedMessageNonce", converters.byteArrayToHexString(res.nonce));
            _this.sendMessage(formData);
          });
        } else {
          encrypted = SSO.encryptNote(_this.messageForm.message, options, _this.messageForm.password || _this.secretPhrase);
          formData.append("encrypt_message", '1');
          formData.append("encryptedMessageData", encrypted.message);
          formData.append("encryptedMessageNonce", encrypted.nonce);
          formData.append("messageToEncryptIsText", 'true');
          formData.append("encryptedMessageIsPrunable", 'true');
          _this.sendMessage(formData);
        }
      } else {
        if (_this.messageForm.isFile) {
          console.log(_this.file);
          formData.append("messageFile", _this.file);
          formData.append("messageIsText", 'false');
          formData.append("messageIsPrunable", 'true');
        } else {
          if (_this.messageForm.message !== "") {
            formData.append("messageIsText", 'true');
            formData.append("message", _this.messageForm.message);
            if (_this.$global.stringToByte(_this.messageForm.message).length >= 28) {    //28 MIN_PRUNABLE_MESSAGE_LENGTH
              formData.append("messageIsPrunable", 'true');
            }
          }
        }
        _this.sendMessage(formData);
      }
    },
    getTransferFee: function () {
      const _this = this;
      let options = {};
      let encrypted = {};
      let formData = new FormData();


      if (_this.transfer.receiver === this.$global.receiverPrefixStr ||
        _this.transfer.receiver === this.$global.receiverEmptyStr) {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
        return;
      }
      if (!_this.transfer.receiver.toUpperCase().match(_this.$global.pattern)) {
        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
        return;
      }

      if (_this.transfer.number === 0) {
        _this.$message.warning(_this.$t('notification.transfer_amount_error'));
        return;
      }
      _this.getAccount(_this.accountInfo.accountRS).then(res => {
        if (typeof res.errorDescription === 'undefined') {
          if (res.errorDescription === "Unknown account") {
            _this.$message.warning(_this.$t('notification.new_account_warning'));
            return;
          }
        }
        _this.accountInfo = res;
        if (_this.transfer.number > _this.accountInfo.effectiveBalanceNQT / _this.$global.unitValue) {
          _this.$message.warning(_this.$t('notification.transfer_balance_insufficient'));
          return;
        }
        if (!(_this.secretPhrase || _this.transfer.password)) {
          _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
          return;
        }
        formData.append("recipient", _this.transfer.receiver);
        formData.append("deadline", "1440");
        formData.append("phased", 'false');
        formData.append("phasingLinkedFullHash", '');
        formData.append("phasingHashedSecret", '');
        formData.append("phasingHashedSecretAlgorithm", '2');
        formData.append("publicKey", res.publicKey);
        formData.append("calculateFee", "true");
        formData.append("broadcast", "false");
        formData.append("feeNQT", "0");
        formData.append("amountNQT", new BigNumber(_this.transfer.number).times(_this.$global.unitValue));


        if (_this.transfer.hasMessage && _this.transfer.message !== "") {
          if (_this.transfer.isEncrypted) {
            if (!(_this.secretPhrase || _this.transfer.password)) {
              _this.$message.warning(_this.$t('notification.sendmessage_null_secret_key'));
              return;
            }
            if (_this.transfer.publicKey === "") {
              _this.$message.warning(_this.$t('notification.transfer_null_public_key'));
              return;
            }
            options.account = _this.transfer.receiver;
            options.publicKey = _this.transfer.publicKey;
            encrypted = SSO.encryptNote(_this.transfer.message, options, _this.secretPhrase || _this.transfer.password);
            formData.append("encrypt_message", '1');
            formData.append("encryptedMessageData", encrypted.message);
            formData.append("encryptedMessageNonce", encrypted.nonce);
            formData.append("messageToEncryptIsText", 'true');
            formData.append("encryptedMessageIsPrunable", 'true');
            // _this.sendMessage(formData);
          } else {
            formData.append("message", _this.transfer.message);
            formData.append("messageIsText", "true");
          }
        }
        _this.sendTransfer(formData);
      });
    },
    encryptFileCallback: function (file, encryptionkeys) {
      return new Promise(function (resolve, reject) {
        SSO.encryptFile(file, encryptionkeys, function (encrypted) {
          resolve(encrypted);
        });
      });
    },
    uploadFile: function () {
      const _this = this;
      let formData = new FormData();
      formData.append("feeNQT", new BigNumber(_this.messageForm.fee).times(_this.$global.unitValue));
      formData.append("secretPhrase", _this.messageForm.password || _this.secretPhrase);
      formData.append("name", _this.messageForm.fileName);
      formData.append("file", _this.storagefile);
      formData.append("deadline", '1440');

      let config = {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      };
      _this.$http.post(_this.$global.urlPrefix() + '?requestType=storeData', formData, config).then(res => {
        if (typeof res.data.errorDescription === 'undefined') {
          if (res.data.broadcasted) {
            _this.$message.success(_this.$t('notification.upload_success'));
            _this.closeDialog();
          } else {
            console.log(res.data);
          }
        } else {
          _this.$message.error(res.data.errorDescription);
        }
      }).catch(err => {
        console.log(err);
      });

    },
    onChain: function () {
      const _this = this;
      _this.preventRepeatedClick();
      let formData = new FormData();
      formData.append("feeNQT", new BigNumber(_this.messageForm.fee).items(_this.$global.unitValue));
      formData.append("secretPhrase", _this.messageForm.password || _this.secretPhrase);
      formData.append("phased", 'false');
      formData.append("phasingLinkedFullHash", '');
      formData.append("phasingHashedSecret", '');
      formData.append("phasingHashedSecretAlgorithm", '2');
      formData.append("name", _this.messageForm.fileName);
      formData.append("file", _this.onchainfile);
      formData.append("deadline", '1440');
      formData.append("onChain", "true");
      formData.append("messageIsText", 'true');
      // formData.append("message", "ceshiyiha");
      let config = {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      };
      _this.$http.post('/sharder?requestType=onChain', formData, config).then(res => {
        if (typeof res.data.errorDescription === 'undefined') {
          if (res.data.broadcasted) {
            _this.$message.success(_this.$t('notification.upload_success'));
            _this.closeDialog();
          } else {
            console.log(res.data);
          }
        } else {
          _this.$message.error(res.data.errorDescription);
        }
      }).catch(err => {
        console.log(err);
      });
    },

    joinNet: function () {
      //todo joinNet
      _this.preventRepeatedClick();
    },
    /**
     * 防止重复点击
     * @param disable
     */
    preventRepeatedClick() {
      const _this = this;
      _this.isDisable = true;
      setTimeout(() => {
        _this.isDisable = false;
      }, 3000)
    },
    sendMessageInfo: function () {
      const _this = this;
      _this.messageForm.executing = true;
      let options = {};
      let encrypted = {};
      let formData = new FormData();
      console.log(_this.messageForm);
      if (_this.messageForm.receiver === this.$global.receiverPrefixStr ||
        _this.messageForm.receiver === this.$global.receiverEmptyStr ||
        _this.messageForm.receiver === this.$global.projectPrefixStr ||
        _this.messageForm.receiver === "") {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
        _this.messageForm.executing = false;
        return;
      }
      if (!_this.messageForm.receiver.toUpperCase().match(_this.$global.pattern)) {
        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
        _this.messageForm.executing = false;
        return;
      }
      if (_this.messageForm.hasPublicKey) {
        if (_this.messageForm.publicKey === "") {
          _this.$message.warning(_this.$t('notification.transfer_null_public_key'));
          _this.messageForm.executing = false;
          return;
        }
      }
      if (!(_this.messageForm.password || _this.secretPhrase)) {
        _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
        _this.messageForm.executing = false;
        return;
      }
      formData.append("recipient", _this.messageForm.receiver);
      formData.append("recipientPublicKey", _this.messageForm.publicKey);
      formData.append("phased", 'false');
      formData.append("phasingLinkedFullHash", '');
      formData.append("phasingHashedSecret", '');
      formData.append("phasingHashedSecretAlgorithm", '2');
      formData.append("feeNQT", new BigNumber(_this.messageForm.fee).items(_this.$global.unitValue));
      formData.append("secretPhrase", _this.messageForm.password || _this.secretPhrase);
      formData.append("deadline", '1440');

      if (!_this.messageForm.isEncrypted) {
        if (_this.file === null) {
          if (_this.messageForm.message !== "") {
            formData.append("messageIsText", 'true');
            formData.append("message", _this.messageForm.message);
            if (_this.$global.stringToByte(_this.messageForm.message).length >= 28) {    //28 MIN_PRUNABLE_MESSAGE_LENGTH
              formData.append("messageIsPrunable", 'true');
            }
          }
        } else {
          formData.append("messageFile", _this.file);
          formData.append("messageIsText", 'false');
          formData.append("messageIsPrunable", 'true');
        }
        _this.sendMessage(formData);
      } else {
        options.account = _this.messageForm.receiver;
        options.publicKey = _this.messageForm.publicKey;

        if (_this.file === null) {
          encrypted = SSO.encryptNote(_this.messageForm.message, options, _this.messageForm.password || _this.secretPhrase);
          formData.append("encrypt_message", '1');
          formData.append("encryptedMessageData", encrypted.message);
          formData.append("encryptedMessageNonce", encrypted.nonce);
          formData.append("messageToEncryptIsText", 'true');
          formData.append("encryptedMessageIsPrunable", 'true');
          _this.sendMessage(formData);
        } else {
          formData.append("messageToEncryptIsText", 'false');
          formData.append("encryptedMessageIsPrunable", 'true');
          let encryptionkeys = SSO.getEncryptionKeys(options, _this.messageForm.password || _this.secretPhrase);
          _this.encryptFileCallback(_this.file, encryptionkeys).then(res => {
            formData.append("encryptedMessageFile", res.file);
            formData.append("encryptedMessageNonce", converters.byteArrayToHexString(res.nonce));
            _this.sendMessage(formData);
          });
        }
      }
    },
    sendMessage: function (formData) {
      const _this = this;
      SSO.sendMessage(formData, function (res) {
        console.log("res", res);
        if (typeof res.errorDescription === 'undefined') {
          if (res.broadcasted) {
            _this.$message.success(_this.$t('notification.transfer_success'));
            _this.closeDialog();
            _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res => {
              _this.$store.commit("setUnconfirmedNotificationsList", res);
            });
          } else {
            console.log(res);
            _this.transfer.fee = res.transactionJSON.feeNQT / _this.$global.unitValue;
          }
        } else {
          if (res.errorDescription.indexOf("$.t") != -1) {
            _this.$message.error(_this.$global.escape2Html(_this.$t(res.errorDescription.slice(3))));
          } else {
            _this.$message.error(_this.$global.escape2Html(res.errorDescription));
          }
        }
        _this.messageForm.executing = false;
      });
      /*return new Promise(function (resolve, reject) {
          let config = {
              headers: {
                  'Content-Type': 'multipart/form-data'
              }
          };
          _this.$http.post('/sharder?requestType=sendMessage', formData, config).then(res => {

              if (typeof res.data.errorDescription === 'undefined') {
                  if (res.data.broadcasted) {
                      _this.$message.success(_this.$t('notification.sendmessage_success'));
                      resolve(res.data);
                      _this.closeDialog();
                      _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res => {
                          _this.$store.commit("setUnconfirmedNotificationsList", res.data);
                      });
                  } else {
                      console.log(res.data);
                      _this.messageForm.fee = res.data.transactionJSON.feeNQT / _this.$global.unitValue;
                      resolve(res.data);
                  }
              } else {
                  _this.$message.error(res.data.errorDescription);
                  resolve(res.data);
              }
          }).catch(err => {
              reject(err);
              console.log(err);
          });
      });*/

    },
    sendBatchMoney(formData) {
      let config = {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      };
      const _this = this;

      return new Promise((resolve, reject) => {
        this.$http.post('/sharder?requestType=airdrop', formData, config).then(function (res) {
          resolve(res.data);
        }).catch(function (err) {
          console.log(err);
        });
      });
    },
    detectionBatchMoney(formData) {
      let config = {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      };
      const _this = this;

      return new Promise((resolve, reject) => {
        this.$http.post('/sharder?requestType=airdropDetection', formData, config).then(function (res) {
          resolve(res.data);
        }).catch(function (err) {
          console.log(err);
        });
      });
    },
    sendBatchTransferInfo: function () {
      const _this = this;
      if (_this.batch_transfer.fileName === "") {
        _this.$message.warning(_this.$t('sso.error_no_file_chosen'));
        return;
      }
      if (_this.batch_transfer.airdropSecretKey === "") {
        _this.$message.warning(_this.$t('sso.error_passphrase_required'));
        return;
      }
      let secretPhrase = JSON.parse(_this.batch_transfer.jsonData).secretPhrase;
      if (!secretPhrase) {
        _this.$message.warning(_this.$t('transfer.airdrop_secret_key_not_found'));
        return;
      }
      _this.batch_transfer.executing = true;
      let formData = new FormData();

      formData.append("jsonString", _this.batch_transfer.jsonData);
      formData.append("key", _this.batch_transfer.airdropSecretKey);


      _this.sendBatchMoney(formData).then(res => {
        if (typeof res.errorDescription === 'undefined') {
          console.log("res", res);
          // res.jsonResult write to JSON file
          _this.$global.funDownload(JSON.stringify(res.jsonResult), _this.batch_transfer.fileName);
          _this.$message.success(_this.$t('transfer.batch_transfer_success'));
          _this.closeDialog();
        } else {
          _this.$message.error(res.errorDescription);
        }
        _this.batch_transfer.executing = false;
      });

    },
    detectionBatchTransferInfo: function () {
      const _this = this;
      if (_this.batch_transfer.fileName === "") {
        _this.$message.warning(_this.$t('sso.error_no_file_chosen'));
        return;
      }
      if (_this.batch_transfer.airdropSecretKey === "") {
        _this.$message.warning(_this.$t('sso.error_passphrase_required'));
        return;
      }
      _this.batch_transfer.executingAnother = true;
      let formData = new FormData();

      formData.append("jsonString", _this.batch_transfer.jsonData);
      formData.append("key", _this.batch_transfer.airdropSecretKey);

      _this.detectionBatchMoney(formData).then(res => {
        if (typeof res.errorDescription === 'undefined') {
          console.log("res", res);
          // res.jsonResult write to JSON file
          _this.$global.funDownload(JSON.stringify(res.jsonResult), _this.batch_transfer.fileName);
          _this.$message.success(_this.$t('transfer.detection_transfer_success'));
          _this.closeDialog();
        } else {
          _this.$message.error(res.errorDescription);
        }
        _this.batch_transfer.executingAnother = false;
      });

    },
    sendTransferInfo: function () {
      const _this = this;
      _this.transfer.executing = true;
      let options = {};
      let encrypted = {};
      let formData = new FormData();
      if (_this.transfer.receiver === this.$global.receiverPrefixStr ||
        _this.transfer.receiver === this.$global.receiverEmptyStr ||
        _this.transfer.receiver === this.$global.projectPrefixStr ||
        _this.transfer.receiver === "") {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
        _this.transfer.executing = false;
        return;
      }
      if (!_this.transfer.receiver.toUpperCase().match(_this.$global.pattern)) {
        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
        _this.transfer.executing = false;
        return;
      }

      if (_this.transfer.receiver === _this.acrossChains.Heco.CosExchangeAddress) {
        _this.$message.warning(_this.$t('acrossChains.no_HMWExchangeAddress'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.receiver === _this.acrossChains.OKEx.CosExchangeAddress) {
        _this.$message.warning(_this.$t('acrossChains.no_OMWExchangeAddress'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.receiver === _this.acrossChains.ETH.CosExchangeAddress) {
        _this.$message.warning(_this.$t('acrossChains.no_EMWExchangeAddress'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.receiver === _this.acrossChains.Tron.CosExchangeAddress) {
        _this.$message.warning(_this.$t('acrossChains.no_TMWExchangeAddress'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.receiver === _this.acrossChains.BSC.CosExchangeAddress) {
        _this.$message.warning(_this.$t('acrossChains.no_BMWExchangeAddress'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.publicKey === "") {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account_public'));
        _this.transfer.executing = false;
        return;
      }
      if (parseFloat(_this.transfer.number, 10) === 0 || _this.transfer.number === "") {
        _this.$message.warning(_this.$t('notification.transfer_amount_error'));
        _this.transfer.executing = false;
        return;
      }
      _this.getAccount(_this.accountInfo.accountRS).then(res => {
        if (res.errorDescription === "Unknown account") {
          _this.$message.warning(_this.$t('notification.new_account_warning'));
          _this.transfer.executing = false;
          return;
        }
        _this.accountInfo = res;

        if (_this.transfer.number.toString().split(".")[1] !== undefined && _this.transfer.number.toString().split(".")[1].length > _this.$global.unitValue.toString().length - 1) {
          _this.$message.warning(_this.$t('notification.transfer_balance_decimal_not_support'));
          _this.transfer.executing = false;
          return;
        }
        if (_this.transfer.number > _this.accountInfo.effectiveBalanceNQT / _this.$global.unitValue) {
          _this.$message.warning(_this.$t('notification.transfer_balance_insufficient'));
          _this.transfer.executing = false;
          return;
        }
        if (!(_this.secretPhrase || _this.transfer.password)) {
          _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
          _this.transfer.executing = false;
          return;
        }
        formData.append("recipient", _this.transfer.receiver);
        formData.append("recipientPublicKey", _this.transfer.publicKey);
        formData.append("deadline", "1440");
        formData.append("phased", 'false');
        formData.append("phasingLinkedFullHash", '');
        formData.append("phasingHashedSecret", '');
        formData.append("phasingHashedSecretAlgorithm", '2');
        formData.append("publicKey", "");
        formData.append("feeNQT", new BigNumber(_this.transfer.fee).times(_this.$global.unitValue));
        formData.append("amountNQT", new BigNumber(_this.transfer.number).times(_this.$global.unitValue));
        formData.append("secretPhrase", _this.secretPhrase || _this.transfer.password);

        if (_this.transfer.hasMessage && _this.transfer.message !== "") {
          if (_this.transfer.isEncrypted) {

            options.account = _this.transfer.receiver;
            options.publicKey = _this.transfer.publicKey;
            encrypted = SSO.encryptNote(_this.transfer.message, options, _this.secretPhrase || _this.transfer.password);
            formData.append("encrypt_message", '1');
            formData.append("encryptedMessageData", encrypted.message);
            formData.append("encryptedMessageNonce", encrypted.nonce);
            formData.append("messageToEncryptIsText", 'true');
            formData.append("encryptedMessageIsPrunable", 'true');
          } else {
            formData.append("message", _this.transfer.message);
            formData.append("messageIsText", "true");
          }
        }
        _this.sendTransfer(formData);
        _this.transfer.executing = false;
      });

    },
    sendExchangeTransferInfo: function () {
      const _this = this;
      _this.preventRepeatedClick();
      _this.transfer.executing = true;
      let options = {};
      let encrypted = {};
      let formData = new FormData();
      var ChainObj;
      switch (_this.chainId) {
        case 1:
          ChainObj = _this.acrossChains.Heco;
          break;
        case 2:
          ChainObj = _this.acrossChains.OKEx;
          break;
        case 3:
          ChainObj = _this.acrossChains.ETH;
          break;
        case 4:
          ChainObj = _this.acrossChains.Tron;
          break;
        case 5:
          ChainObj = _this.acrossChains.BSC;
          break;
      }
      if (ChainObj.CosExchangeAddress === this.$global.receiverPrefixStr ||
        ChainObj.CosExchangeAddress === this.$global.receiverEmptyStr ||
        ChainObj.CosExchangeAddress === this.$global.projectPrefixStr ||
        ChainObj.CosExchangeAddress === ""
      ) {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account'));
        _this.transfer.executing = false;
        return;
      }
      if (!ChainObj.CosExchangeAddress.toUpperCase().match(_this.$global.pattern)) {
        _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
        _this.transfer.executing = false;
        return;
      }
      if (ChainObj.CosExchangeAddressPublicKey === "") {
        _this.$message.warning(_this.$t('notification.sendmessage_null_account_public'));
        _this.transfer.executing = false;
        return;
      }
      if (_this.transfer.exchangeNumber === 0) {
        _this.$message.warning(_this.$t('notification.transfer_amount_error'));
        _this.transfer.executing = false;
        return;
      }
      _this.getAccount(_this.accountInfo.accountRS).then(res => {
        if (typeof res.errorDescription === 'undefined') {
          if (res.errorDescription === "Unknown account") {
            _this.$message.warning(_this.$t('notification.new_account_warning'));
            _this.transfer.executing = false;
            return;
          }
        }
        _this.accountInfo = res;

        if (_this.transfer.exchangeNumber.toString().split(".")[1] !== undefined && _this.transfer.exchangeNumber.toString().split(".")[1].length > _this.$global.unitValue.toString().length - 1) {
          _this.$message.warning(_this.$t('notification.transfer_balance_decimal_not_support'));
          _this.transfer.executing = false;
          return;
        }
        if (_this.transfer.exchangeNumber > ChainObj.convertible_balance * ChainObj.CosExchangeRate) {
          _this.$message.warning(_this.$t('acrossChains.convertible_balance_not_enough'));
          _this.transfer.executing = false;
          return;
        }
        if (_this.transfer.exchangeNumber > _this.accountInfo.effectiveBalanceNQT / _this.$global.unitValue) {
          _this.$message.warning(_this.$t('notification.transfer_balance_insufficient'));
          _this.transfer.executing = false;
          return;
        }
        if (!(_this.secretPhrase || _this.transfer.password)) {
          _this.$message.warning(_this.$t('notification.transfer_null_secret_key'));
          _this.transfer.executing = false;
          return;
        }
        formData.append("recipient", ChainObj.CosExchangeAddress);
        formData.append("recipientPublicKey", ChainObj.CosExchangeAddressPublicKey);
        formData.append("deadline", "1440");
        formData.append("phased", 'false');
        formData.append("phasingLinkedFullHash", '');
        formData.append("phasingHashedSecret", '');
        formData.append("phasingHashedSecretAlgorithm", '2');
        formData.append("publicKey", "");
        formData.append("feeNQT", _this.transfer.fee * _this.$global.unitValue);
        formData.append("amountNQT", _this.transfer.exchangeNumber * _this.$global.unitValue);
        formData.append("secretPhrase", _this.secretPhrase || _this.transfer.password);

        formData.append("chainId", _this.chainId);
        if (_this.transfer.hasMessage && _this.transfer.message !== "") {
          if (_this.transfer.isEncrypted) {

            options.account = ChainObj.CosExchangeAddress;
            options.publicKey = ChainObj.CosExchangeAddressPublicKey;
            encrypted = SSO.encryptNote(_this.transfer.message, options, _this.secretPhrase || _this.transfer.password);
            formData.append("encrypt_message", '1');
            formData.append("encryptedMessageData", encrypted.message);
            formData.append("encryptedMessageNonce", encrypted.nonce);
            formData.append("messageToEncryptIsText", 'true');
            formData.append("encryptedMessageIsPrunable", 'true');
          } else {
            formData.append("message", _this.transfer.message);
            formData.append("messageIsText", "true");
          }
        }
        _this.sendTransfer(formData);
        _this.transfer.executing = false;
      });

    },
    /**
     * 最大今日可兑换量
     */
    setMaxConvertibleBalance: function () {
      var ChainObj;
      const _this = this;
      switch (_this.chainId) {
        case 1:
          ChainObj = _this.acrossChains.Heco;
          break;
        case 2:
          ChainObj = _this.acrossChains.OKEx;
          break;
        case 3:
          ChainObj = _this.acrossChains.ETH;
          break;
        case 4:
          ChainObj = _this.acrossChains.Tron;
          break;
        case 5:
          ChainObj = _this.acrossChains.BSC;
          break;
        default:
          break;
      }
      _this.transfer.exchangeNumber = ChainObj.convertible_balance  < _this.acrossChains.balance - 1 ?
          ChainObj.convertible_balance  : _this.acrossChains.balance -1;
    },

    setMediumConvertibleBalance: function () {
      var ChainObj;
      const _this = this;
      switch (_this.chainId) {
          case 1:
              ChainObj = _this.acrossChains.Heco;
              break;
          case 2:
              ChainObj = _this.acrossChains.OKEx;
              break;
          case 3:
              ChainObj = _this.acrossChains.ETH;
              break;
          case 4:
              ChainObj = _this.acrossChains.Tron;
              break;
          case 5:
              ChainObj = _this.acrossChains.BSC;
              break;
          default:
              break;
      }
      _this.transfer.exchangeNumber = ChainObj.convertible_balance / 2  < (_this.acrossChains.balance - 1) / 2 ?
        ChainObj.convertible_balance / 2 : (_this.acrossChains.balance - 1) / 2;
    },

    sendTransfer: function (formData) {
      const _this = this;
      SSO.sendMoney(formData, function (res) {
        if (formData.recipient === _this.acrossChains.Heco.CosExchangeAddress) {
          _this.acrossChains.Heco.convertible_balance -= this.transfer.exchangeNumber / _this.acrossChains.Heco.CosExchangeRate;
          _this.acrossChains.Heco.target_balance += this.transfer.exchangeNumber * _this.acrossChains.Heco.CosExchangeRate;
        } else if (formData.recipient === _this.acrossChains.OKEx.CosExchangeAddress) {
          _this.acrossChains.OKEx.convertible_balance -= this.transfer.exchangeNumber / _this.acrossChains.OKEx.CosExchangeRate;
          _this.acrossChains.OKEx.target_balance += this.transfer.exchangeNumber * _this.acrossChains.OKEx.CosExchangeRate;
        } else if (formData.recipient === _this.acrossChains.ETH.CosExchangeAddress) {
          _this.acrossChains.ETH.convertible_balance -= this.transfer.exchangeNumber / _this.acrossChains.ETH.CosExchangeRate;
          _this.acrossChains.ETH.target_balance += this.transfer.exchangeNumber * _this.acrossChains.ETH.CosExchangeRate;
        } else if (formData.recipient === _this.acrossChains.Tron.CosExchangeAddress) {
          _this.acrossChains.Tron.convertible_balance -= this.transfer.exchangeNumber / _this.acrossChains.Tron.CosExchangeRate;
          _this.acrossChains.Tron.target_balance += this.transfer.exchangeNumber * _this.acrossChains.Tron.CosExchangeRate;
        } else if (formData.recipient === _this.acrossChains.BSC.CosExchangeAddress) {
          _this.acrossChains.BSC.convertible_balance -= this.transfer.exchangeNumber / _this.acrossChains.BSC.CosExchangeRate;
          _this.acrossChains.BSC.target_balance += this.transfer.exchangeNumber * _this.acrossChains.BSC.CosExchangeRate;
        }

        if (typeof res.errorDescription === 'undefined') {
          if (res.broadcasted) {
            _this.$message.success(_this.$t('notification.transfer_success'));
            _this.closeDialog();
            _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res => {
              _this.$store.commit("setUnconfirmedNotificationsList", res);
            });
          } else {
            _this.transfer.fee = res.transactionJSON.feeNQT / _this.$global.unitValue;
          }
        } else {
          if (res.errorDescription.indexOf("$.t") != -1) {
            _this.$message.error(_this.$global.escape2Html(_this.$t(res.errorDescription.slice(3))));
          } else {
            _this.$message.error(_this.$global.escape2Html(res.errorDescription));
          }
        }
        _this.transfer.executing = false;
      });

      /*return new Promise(function (resolve, reject) {
          let config = {
              headers: {
                  'Content-Type': 'multipart/form-data'
              }
          };
          _this.$http.post('/sharder?requestType=sendMoney', formData, config).then(res => {
              if (typeof res.data.errorDescription === 'undefined') {
                  if (res.data.broadcasted) {
                      _this.$message.success(_this.$t('notification.transfer_success'));
                      resolve(res.data);
                      _this.closeDialog();
                      _this.$global.setUnconfirmedTransactions(_this, SSO.account).then(res => {
                          _this.$store.commit("setUnconfirmedNotificationsList", res.data);
                      });
                  } else {
                      console.log(res.data);
                      _this.transfer.fee = res.data.transactionJSON.feeNQT / _this.$global.unitValue;
                      resolve(res.data);
                  }
              } else {
                  _this.$message.error(res.data.errorDescription);
                  resolve(res.data);
              }
          }).catch(err => {
              reject(err);
              console.log(err);
          });

      });*/
    },
    getAccountTransactionList: function () {
      const _this = this;
      let params = new URLSearchParams();

      params.append("account", _this.accountInfo.accountRS);

      _this.unconfirmedTransactionsList = _this.$store.state.unconfirmedTransactionsList.unconfirmedTransactions;

      let i = 0;
      if (typeof _this.unconfirmedTransactionsList !== 'undefined') {
        i = _this.unconfirmedTransactionsList.length;
      }

      params.append("firstIndex", (_this.currentPage - 1) * _this.pageSize);
      params.append("lastIndex", _this.currentPage * _this.pageSize - 1 - i);

      if (_this.selectType === 1.5) {
        params.append("type", "1");
        params.append("subtype", "5");
      } else if (_this.selectType === 1) {
        params.append("type", "1");
        params.append("subtype", "0");
      }

      _this.loading = true;
      this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactions', { params }).then(function (res1) {
        _this.accountTransactionList = res1.data.transactions;
        _this.totalSize = res1.data.count;
        // params.delete("firstIndex");
        // params.delete("lastIndex");
        // _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactionsCount', {params}).then(function (res2) {
        //
        //     if (typeof res2.data.errorDescription === "undefined") {
        //         _this.totalSize = res2.data.count;
        //     } else {
        //         _this.$message.error(res2.data.errorCode);
        //     }
        // }).catch(err => {
        //     // _this.$message.error(err.message);
        // });
        _this.loading = false;
        _this.getTotalList();
        _this.getDrawData();
        _this.updateMinerState();
      }).catch(function (err) {
        _this.loading = false;
      });
    },
    /**
       * 得到兑换交易信息
       */
    getAccountExchangeTransactionList: function () {
      const _this = this;
      let params = new URLSearchParams();

      params.append("account", _this.accountInfo.accountRS);

      _this.unconfirmedTransactionsList =
        _this.$store.state.unconfirmedTransactionsList.unconfirmedTransactions;

      let i = 0;
      if (typeof _this.unconfirmedTransactionsList !== "undefined") {
        i = _this.unconfirmedTransactionsList.length;
      }

      params.append("firstIndex", (_this.currentPage - 1) * _this.pageSize);
      params.append("lastIndex", _this.currentPage * _this.pageSize - 1 - i);

      if (_this.exchangeSelectType === 1) {
        params.append(
          "recipientRS",
          _this.acrossChains.Heco.CosExchangeAddress
        );
      } else if (_this.exchangeSelectType === 2) {
        params.append("senderRS", _this.acrossChains.Heco.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 3) {
        params.append(
          "recipientRS",
          _this.acrossChains.OKEx.CosExchangeAddress
        );
      } else if (_this.exchangeSelectType === 4) {
        params.append("senderRS", _this.acrossChains.OKEx.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 5) {
        params.append("recipientRS", _this.acrossChains.ETH.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 6) {
        params.append("senderRS", _this.acrossChains.ETH.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 7) {
        params.append(
          "recipientRS",
          _this.acrossChains.Tron.CosExchangeAddress
        );
      } else if (_this.exchangeSelectType === 8) {
        params.append("senderRS", _this.acrossChains.Tron.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 9) {
        params.append("recipientRS", _this.acrossChains.BSC.CosExchangeAddress);
      } else if (_this.exchangeSelectType === 10) {
        params.append("senderRS", _this.acrossChains.BSC.CosExchangeAddress);
      }

      _this.loading = true;
      this.$http
        .get(
          _this.$global.urlPrefix() + "?requestType=getBlockchainTransactions",
          { params }
        )
        .then(function (res1) {
          _this.accountTransactionList = res1.data.transactions;
          _this.totalSize = res1.data.count;
          // params.delete("firstIndex");
          // params.delete("lastIndex");
          // _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactionsCount', {params}).then(function (res2) {
          //
          //     if (typeof res2.data.errorDescription === "undefined") {
          //         _this.totalSize = res2.data.count;
          //     } else {
          //         _this.$message.error(res2.data.errorCode);
          //     }
          // }).catch(err => {
          //     // _this.$message.error(err.message);
          // });
          _this.loading = false;
          _this.getTotalList();
          _this.getDrawData();
          _this.updateMinerState();
        })
        .catch(function (err) {
          _this.loading = false;
        });
    },
    updateMinerState() {
      let _this = this;
      _this.accountTransactionList.forEach(val => {
        if (val.type === 8 && val.confirmations) {
          _this.$store.state.quitPool[val.attachment.poolId] = undefined;
          _this.$store.state.destroyPool[val.attachment.poolId] = undefined;
        }
      });
    },
    clearHubSetting() {
      let _this = this;
      _this.hubsetting.address = '';
      _this.hubsetting.port = '';
      _this.hubsetting.clientSecretkey = '';
      _this.hubsetting.publicAddress = '';
      _this.hubsetting.SS_Address = '';
      _this.hubsetting.register_status = '';
      _this.hubsetting.register_status_text = '';
      _this.needRegister = false;
    },
    openSendMessageDialog: function () {
      if (SSO.downloadingBlockchain) {
        this.$message.warning(this.$t("account.synchronization_block"));
        return;
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.sendMessageDialog = true;
    },
    openStorageFileDialog: function () {
      if (SSO.downloadingBlockchain) {
        this.$message.warning(this.$t("account.synchronization_block"));
        return;
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.storageFileDialog = true;
    },
    openOnChainDialog: function () {
      if (SSO.downloadingBlockchain) {
        this.$message.warning(this.$t("account.synchronization_block"));
        return;
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.onChainDialog = true;
    },
    openJoinNetDialog: function () {
      if (SSO.downloadingBlockchain) {
        this.$message.warning(this.$t("account.synchronization_block"));
        return;
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.joinNetDialog = true;
    },
    /**
     * 得到系统跨链兑换地址信息
     */
    getAcrossAddress: function () {
      const _this = this;
      if(!this.$global.acrossChainExchangeEnable){
          _this.exchangeOpenButton = false;
          return;
      }

      _this.$http.get(window.api.getAcrossAddress).then(function (res1) {
        var result = res1.data.body;
        if (result) {
          _this.acrossChains.Heco.CosExchangeAddress = result.Heco.CosExchangeAddress;
          _this.acrossChains.Heco.CosExchangeAddressPublicKey = result.Heco.CosExchangeAddressPublicKey;
          _this.acrossChains.Heco.CosExchangeRate = result.Heco.CosExchangeRate;
          _this.acrossChains.Heco.ExchangeAddress = result.Heco.ExchangeAddress;
          _this.acrossChains.Heco.contractAddress = result.Heco.contractAddress;

          _this.acrossChains.OKEx.CosExchangeAddress = result.OKEx.CosExchangeAddress;
          _this.acrossChains.OKEx.CosExchangeAddressPublicKey = result.OKEx.CosExchangeAddressPublicKey;
          _this.acrossChains.OKEx.CosExchangeRate = result.OKEx.CosExchangeRate;
          _this.acrossChains.OKEx.ExchangeAddress = result.OKEx.ExchangeAddress;
          _this.acrossChains.OKEx.contractAddress = result.OKEx.contractAddress;

          _this.acrossChains.ETH.CosExchangeAddress = result.ETH.CosExchangeAddress;
          _this.acrossChains.ETH.CosExchangeAddressPublicKey = result.ETH.CosExchangeAddressPublicKey;
          _this.acrossChains.ETH.CosExchangeRate = result.ETH.CosExchangeRate;
          _this.acrossChains.ETH.ExchangeAddress = result.ETH.ExchangeAddress;
          _this.acrossChains.ETH.contractAddress = result.ETH.contractAddress;

          _this.acrossChains.Tron.CosExchangeAddress = result.Tron.CosExchangeAddress;
          _this.acrossChains.Tron.CosExchangeAddressPublicKey = result.Tron.CosExchangeAddressPublicKey;
          _this.acrossChains.Tron.CosExchangeRate = result.Tron.CosExchangeRate;
          _this.acrossChains.Tron.ExchangeAddress = result.Tron.ExchangeAddress;
          _this.acrossChains.Tron.contractAddress = result.Tron.contractAddress;

          _this.acrossChains.BSC.CosExchangeAddress = result.BSC.CosExchangeAddress;
          _this.acrossChains.BSC.CosExchangeAddressPublicKey = result.BSC.CosExchangeAddressPublicKey;
          _this.acrossChains.BSC.CosExchangeRate = result.BSC.CosExchangeRate;
          _this.acrossChains.BSC.ExchangeAddress = result.BSC.ExchangeAddress;
          _this.acrossChains.BSC.contractAddress = result.BSC.contractAddress;
          if(_this.projectName !== 'sharder'){
            _this.exchangeOpenButton = false;
          }
        }
      }).catch(err => {
          _this.$message.error(_this.$t('acrossChains.error'));
      });

    },
    /**
     * 打开跨链资产页面
     */
    openAssetsAcrossChainsDialog: function () {
      const _this = this;

      if (typeof (this.secretPhrase) === 'undefined') {
        this.$message.warning(this.$t("acrossChains.use_secretPhrase_tip"));
        return;
      }
      if (SSO.downloadingBlockchain) {
        this.$message.warning(this.$t("account.synchronization_block"));
        return;
      }

      // if(!this.CosExchangeAddressPublicKey){
      //     _this.$message.info(_this.$t('acrossChains.not_yet_open'));
      //     return;
      // }
      //发起网关请求，查找当前帐号绑定的信息

      var str = _this.$global.formatNQTMoney(_this.accountInfo.effectiveBalanceNQT, 2);
      _this.acrossChains.balance = parseFloat(str.substring(0, str.length - 2));

      if (_this.accountInfo.accountRS) {
        _this.$http.get(window.api.getAccountInfoUrl, { params: { accountRS: _this.accountInfo.accountRS } }).then(function (res1) {
          switch (res1.data.code) {
            case "200":
              var account = res1.data.body;
              console.log(account)
              _this.acrossChains.Heco.target_address = account.HecoAddress;
              _this.acrossChains.Heco.old_address = account.HecoAddress;
              _this.acrossChains.Heco.target_balance = account.HecoBalance / _this.$global.acrossUnitVal.HECO;
              _this.acrossChains.Heco.convertible_balance = account.HecoConvertibleQuantity / _this.$global.acrossUnitVal.HECO;

              _this.acrossChains.OKEx.target_address = account.OKExAddress;
              _this.acrossChains.OKEx.old_address = account.OKExAddress;
              _this.acrossChains.OKEx.target_balance = account.OKExBalance / _this.$global.acrossUnitVal.OKEX;
              _this.acrossChains.OKEx.convertible_balance = account.OKExConvertibleQuantity / _this.$global.acrossUnitVal.OKEX;

              _this.acrossChains.ETH.target_address = account.ETHAddress;
              _this.acrossChains.ETH.old_address = account.ETHAddress;
              _this.acrossChains.ETH.target_balance = account.ETHBalance / _this.$global.acrossUnitVal.ETH;
              _this.acrossChains.ETH.convertible_balance = account.ETHConvertibleQuantity / _this.$global.acrossUnitVal.ETH;

              _this.acrossChains.Tron.target_address = account.TronAddress;
              _this.acrossChains.Tron.old_address = account.TronAddress;
              _this.acrossChains.Tron.target_balance = account.TronBalance / _this.$global.acrossUnitVal.TRON;
              _this.acrossChains.Tron.convertible_balance = account.TronConvertibleQuantity / _this.$global.acrossUnitVal.TRON;

              _this.acrossChains.BSC.target_address = account.BSCAddress;
              _this.acrossChains.BSC.old_address = account.BSCAddress;
              _this.acrossChains.BSC.target_balance = account.BSCBalance / _this.$global.acrossUnitVal.BSC;
              _this.acrossChains.BSC.convertible_balance = account.BSCConvertibleQuantity / _this.$global.acrossUnitVal.BSC;

              _this.acrossChains.id = account.id;

              _this.$store.state.mask = true;
              _this.AssetsAcrossChainsDialog = true;
              break;
            case "209":
              _this.$message.warning(_this.$t('acrossChains.bindAddress_incomplete'));
              break;
            case "363":
              _this.$message.warning(_this.$t('acrossChains.bindAddress_double'));
              break;
            default:
              break;
          }
        }).catch(err => {
          _this.$message.error(_this.$t('acrossChains.error'));
        });
      } else {
        _this.$message.error(_this.$t('acrossChains.no_accountId'));
      }

    },
    openExchangeDialog: function () {
      if (SSO.downloadingBlockchain) {
        return this.$message.warning(this.$t("account.synchronization_block"));
      }

      switch (this.chainId) {
        case 1:
          if (this.acrossChains.Heco.target_address === "") {
            return this.$message.warning(
              this.$t("acrossChains.no_HecoAddress")
            );
          }
          break;
        case 2:
          if (this.acrossChains.OKEx.target_address === "") {
            return this.$message.warning(
              this.$t("acrossChains.no_OKExAddress")
            );
          }
          break;
        case 3:
          if (this.acrossChains.ETH.target_address === "") {
            return this.$message.warning(this.$t("acrossChains.no_ETHAddress"));
          }
          break;
        case 4:
          if (this.acrossChains.Tron.target_address === "") {
            return this.$message.warning(
              this.$t("acrossChains.no_TronAddress")
            );
          }
          break;
        case 5:
          if (this.acrossChains.BSC.target_address === "") {
            return this.$message.warning(this.$t("acrossChains.no_BSCAddress"));
          }
          break;
        default:
          break;
      }
      this.$store.state.mask = true;
      this.AssetsExchangeDialog = true;
      this.AssetsAcrossChainsDialog = false;
      this.transfer.executing = false;
    },
    formatInputDiskCapacity: function (val) {
      return val + " T";
    },
    calculatePledge: function (val) {
      this.diskPledge = val * 133;
    },
    calPledgeCapacityByBalance: function () {
      const _this = this;
      let accountBalance = _this.accountInfo.effectiveBalanceNQT / _this.$global.unitValue;
      let val = Math.ceil(accountBalance / 133);
      if (val <= 192) {
        this.capacity = val;
      } else {
        this.capacity = 192;
      }
    },
    formatDiskCapacity: function () {
      return parseFloat(this.userConfig.diskCapacity / 1024 / 1024).toFixed(2) + " GB";
    },
    openTransferDialog: function () {
      if (SSO.downloadingBlockchain) {
        return this.$message.warning(this.$t("account.synchronization_block"));
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.tranferAccountsDialog = true;
      this.transfer.executing = false;
    },
    openBatchTransferDialog: function () {
      if (SSO.downloadingBlockchain) {
        return this.$message.warning(this.$t("account.synchronization_block"));
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      this.$store.state.mask = true;
      this.batchTranferAccountsDialog = true;
      this.batch_transfer.executing = false;
      this.batch_transfer.executingAnother = false;
    },
    openHubSettingDialog: function () {
      const _this = this;
      _this.getLatestHubVersion();
      _this.$store.state.mask = true;
      _this.hubSettingDialog = true;
    },
    openHubInitDialog: function () {
      const _this = this;
      _this.$store.state.mask = true;
      _this.hubInitDialog = true;
    },
    openUseNATDialog() {
      const _this = this;
      if (this.whetherShowUseNATServiceBtn()) {
        _this.operationType = 'initNormal';
      } else {
        _this.operationType = 'reConfigNormal';
      }
      _this.$store.state.mask = true;
      _this.useNATServiceDialog = true;
    },
    openTradingInfoDialog: function (trading) {
      this.trading = trading;
      this.tradingInfoDialog = true;
    },
    mobileViewDetail: function (trading) {
      if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
        this.openTradingInfoDialog(trading)
      }
    },
    isUserInfoDialog: function (bool) {
      this.userInfoDialog = bool;
      this.$store.state.mask = bool;
    },
    openAccountInfoDialog: function (account) {
      this.generatorRS = account;
      this.accountInfoDialog = true;
    },
    openBlockInfoDialog: function (height) {
      this.height = height;
      this.blockInfoDialog = true;
    },
    openAdminDialog: function (title) {
      const _this = this;
      _this.adminPasswordTitle = title;

      this.$refs["reconfigureForm"].clearValidate();
      this.$refs["initForm"].clearValidate();

      let validationPassed;
      let formName = '';
      if (title === 'reConfig') {
        this.operationType = 'reConfig';
        _this.$refs['reconfigureForm'].validate((valid) => {
          if (valid) {
            let hubSettingFormData = _this.verifyAndGenerateHubSettingFormData();
            if (hubSettingFormData) {
              _this.params = hubSettingFormData;
              _this.hubSettingDialog = false;
              _this.adminPasswordDialog = true;
            }
          } else {
            return false;
          }
        });
      } else if (title === 'resetNormal') {
        this.operationType = 'resetNormal';
        formName = 'useNATForm';
      } else {
        this.operationType = 'init';
        validationPassed = true;
      }

      // validate the parameters of the form
      if (formName.length > 1) {
        _this.$refs[formName].validate((valid) => {
          validationPassed = valid;
        });
      }

      // set the dialog state and visible
      if (validationPassed) {
        _this.hubSettingDialog = false;
        _this.useNATServiceDialog = false;
        _this.adminPasswordDialog = true;
      }
    },
    openSecretPhraseDialog: function () {
      const _this = this;
      if (SSO.downloadingBlockchain) {
        _this.$message.warning(_this.$t("account.synchronization_block"));
        return;
      }
      // if(this.blockchainState.blockchainState != "UP_TO_DATE"){
      //     this.$message.warning(this.$t("account.up_to_date"));
      //     return;
      // }
      _this.isUserInfoDialog(false);
      _this.secretPhrase ? _this.setName(_this.secretPhrase) : _this.secretPhraseDialog = true;
    },
    getAdminPassword: function (adminPwd) {
      const _this = this;
      _this.adminPassword = adminPwd;
      _this.adminPasswordDialog = false;
      if (_this.adminPasswordTitle === 'reset'
        || _this.adminPasswordTitle === 'resetNormal') {
        _this.resetHub(adminPwd, 'reset');
      } else if (_this.adminPasswordTitle === 'factoryReset') {
        _this.resetHub(adminPwd, 'factoryReset');
      } else if (_this.adminPasswordTitle === 'restart') {
        _this.restartHub(adminPwd);
      } else if (_this.adminPasswordTitle === 'update') {
        _this.updateHubVersion(adminPwd);
      } else if (_this.adminPasswordTitle === 'reConfig') {
        _this.params.append("adminPassword", adminPwd);
        _this.updateHubSetting(_this.params);
      }
    },
    getSecretPhrase: function (secretPhrase) {
      const _this = this;
      _this.secretPhraseDialog = false;
      _this.setName(secretPhrase);
    },
    closeDialog: function () {
      this.$refs["reconfigureForm"].clearValidate();
      this.$refs["useNATForm"].clearValidate();
      this.$refs["initForm"].clearValidate();

      // clear dialog form fields
      if (this.hubSettingDialog && this.$refs['reconfigureForm']) {
        // do not reset fields, otherwise the setting button will hide
        // because "this.hubsetting.SS_Address" is used to display judgment
        this.$refs["reconfigureForm"].resetFields();
      }
      /*console.info("this.$refs['initForm']:"+this.$refs['initForm']);
      if (this.hubInitDialog && this.$refs['initForm']) {
          this.$refs["initForm"].resetFields();
          console.log("form名称:"+this.$refs["initForm"].resetFields());
      }*/
      if (this.useNATServiceDialog && this.$refs['useNATForm']) {
        this.$refs["useNATForm"].resetFields();
      }

      this.sendSuccess = false;
      this.hubsetting.registerSiteAccount = false;
      this.registerSharderSiteUser.sharderAccountPhoneOrEmail = "";
      this.registerSharderSiteUser.verificationCode = "";
      this.registerSharderSiteUser.setSharderPwd = "";
      this.registerSharderSiteUser.confirmSharderPwd = "";
      this.registerSharderSiteUser.pictureVerificationCode = "";

      this.operationType = 'init';


      this.$store.state.mask = false;
      this.sendMessageDialog = false;
      this.storageFileDialog = false;
      this.onChainDialog = false;
      this.joinNetDialog = false;
      this.AssetsAcrossChainsDialog = false;
      if (this.AssetsExchangeDialog) {
        this.AssetsExchangeDialog = false;
        this.AssetsAcrossChainsDialog = true;
      }

      this.capacity = 0;
      this.accountSecret = "";
      this.mortgageFee = 0;
      this.tranferAccountsDialog = false;
      this.batchTranferAccountsDialog = false;
      this.hubSettingDialog = false;
      this.hubInitDialog = false;
      this.tradingInfoDialog = false;
      this.accountInfoDialog = false;
      this.userInfoDialog = false;
      this.useNATServiceDialog = false;

      const _this = this;
      _this.messageForm.errorCode = false;
      _this.messageForm.receiver = this.$global.receiverPrefixStr;
      _this.messageForm.message = "";
      _this.messageForm.isEncrypted = false;
      _this.messageForm.hasPublicKey = false;
      _this.messageForm.isFile = false;
      _this.messageForm.publicKey = "";
      _this.messageForm.senderPublickey = SSO.publicKey;
      _this.messageForm.fileName = "";
      _this.messageForm.password = "";
      _this.messageForm.fee = 1;
      _this.file = null;
      _this.storagefile = null;
      _this.parsefile = null;
      _this.transfer.receiver = this.$global.receiverPrefixStr;
      _this.transfer.number = 0;
      _this.transfer.fee = 1;
      _this.transfer.hasMessage = false;
      _this.transfer.message = "";
      _this.transfer.isEncrypted = false;
      _this.transfer.password = "";
      _this.transfer.hasPublicKey = false;
      _this.transfer.publicKey = "";
      _this.transfer.errorCode = false;

      _this.isShowName = true;
      _this.temporaryName = "";
      _this.needRegister = false;
      _this.hubsetting.register_status = '';
      _this.hubsetting.register_status_text = '';

      _this.hubsetting.initialSerialClickCount = 0;
      _this.hubsetting.settingSerialClickCount = 0;
      _this.hubsetting.natSerialClickCount = 0;

      _this.hubsetting.executing = false;
    },
    copySuccess: function () {
      const _this = this;
      _this.$message({
        showClose: true,
        message: _this.$t('notification.clipboard_success'),
        type: "success"
      });
    },
    setName: function (secretPhrase) {
      const _this = this;
      let formData = new FormData();
      formData.append("name", _this.temporaryName);
      formData.append("secretPhrase", secretPhrase);
      formData.append("deadline", "1440");
      formData.append("phased", "false");
      formData.append("phasingLinkedFullHash", "");
      formData.append("phasingHashedSecret", "");
      formData.append("phasingHashedSecretAlgorithm", "2");
      formData.append("feeNQT", "0");

      /*_this.$http.post(_this.$global.urlPrefix() + '?requestType=setAccountInfo', formData).then(res => {
          if (typeof res.data.errorDescription === "undefined") {
              _this.$message.success(_this.$t('notification.modify_success'));
              _this.accountInfo.name = res.data.transactionJSON.attachment.name;
              _this.isShowName = true;
              _this.temporaryName = "";
          } else {
              _this.$message.error(res.data.errorDescription);
              _this.accountInfo.name = "";
              _this.isShowName = true;
          }
      })*/
      SSO.setAccountInfo(formData, function (res) {
        console.log("res", res);
        if (typeof res.errorDescription === "undefined") {
          _this.$message.success(_this.$t('notification.modify_success'));
          _this.accountInfo.name = res.data.transactionJSON.attachment.name;
          _this.isShowName = true;
          _this.temporaryName = "";
        } else {
          if (res.errorDescription.indexOf("$.t") != -1) {
            _this.$message.error(_this.$global.escape2Html(_this.$t(res.errorDescription.slice(3))));
          } else {
            _this.$message.error(_this.$global.escape2Html(res.errorDescription));
          }
          _this.accountInfo.name = "";
          _this.isShowName = true;
        }
      });
    },
    copyError: function () {
      const _this = this;
      _this.$message({
        showClose: true,
        message: _this.$t('notification.clipboard_error'),
        type: "error"
      });
    },
    delFile: function () {
      const _this = this;
      $('#file').val("");
      _this.messageForm.fileName = "";
      _this.file = null;
      _this.messageForm.isFile = false;
    },
    delStorageFile: function () {
      const _this = this;
      $('#storageFile').val("");
      _this.messageForm.fileName = "";
      _this.storagefile = null;
      _this.messageForm.isFile = false;
    },
    delParseFile: function () {
      const _this = this;
      $('#parseFile').val("");
      _this.batch_transfer.fileName = "";
      _this.parsefile = null;
      _this.batch_transfer.isFile = false;
    },
    delOnChainFile: function () {
      const _this = this;
      $('#onChainFile').val("");
      _this.messageForm.fileName = "";
      _this.onchainfile = null;
      _this.messageForm.isFile = false;
    },
    fileChange: function (e) {
      const _this = this;
      _this.messageForm.fileName = e.target.files[0].name;
      _this.file = document.getElementById("file").files[0];

      if (_this.file.size > 1024 * 1024 * 5) {
        _this.delFile();
        _this.$message.error(_this.$t('notification.file_exceeds_max_limit'));
        return;
      }
      _this.messageForm.isFile = true;
      _this.messageForm.message = "";
    },
    storageFileChange: function (e) {
      const _this = this;
      _this.messageForm.fileName = e.target.files[0].name;
      _this.storagefile = document.getElementById("storageFile").files[0];
      console.log(_this.storagefile);
      if (_this.storagefile.size > 1024 * 1024 * 5) {
        _this.delStorageFile();
        _this.$message.error(_this.$t('notification.file_exceeds_max_limit'));
        return;
      }
      _this.messageForm.isFile = true;
      _this.messageForm.message = "";
    },
    parseFileChange: function (e) {
      const _this = this;
      _this.batch_transfer.fileName = e.target.files[0].name;
      _this.parsefile = document.getElementById("parseFile").files[0];
      if (_this.parsefile.type != "application/json") {
        _this.$message.error(_this.$t('notification.unsupported_file_type'));
        return;
      }
      if (_this.parsefile.size > 1024 * 1024 * 5) {
        _this.delParseFile();
        _this.$message.error(_this.$t('notification.file_exceeds_max_limit'));
        return;
      }
      _this.batch_transfer.isFile = true;
      _this.batch_transfer.message = "";
      _this.$global.readFile(_this.parsefile).then(res => _this.batch_transfer.jsonData = res);
      console.log("_this.batch_transfer", _this.batch_transfer);
    },

    onChainFileChange: function (e) {
      const _this = this;
      _this.messageForm.fileName = e.target.files[0].name;
      _this.onchainfile = document.getElementById("onChainFile").files[0];
      console.log(_this.onchainfile);
      if (_this.onchainfile.size > 1024 * 1024 * 10) {
        _this.delOnChainFile();
        _this.$message.error(_this.$t('notification.file_exceeds_max_limit'));
        return;
      }
      _this.messageForm.isFile = true;
      _this.messageForm.message = "";
    },
    isClose: function () {
      const _this = this;
      _this.tradingInfoDialog = false;
      _this.accountInfoDialog = false;
      _this.blockInfoDialog = false;
      _this.adminPasswordDialog = false;
      _this.secretPhraseDialog = false;
      _this.isShowName = true;
      _this.temporaryName = '';
    },
    versionCompare(current, latest) {
      let currentPre = parseFloat(current);
      let latestPre = parseFloat(latest);
      let currentNext = current.replace(currentPre + ".", "");
      let latestpreNext = latest.replace(latestPre + ".", "");
      if (currentPre > latestPre) {
        return false;
      } else if (currentPre < latestPre) {
        return true;
      } else {
        if (currentNext >= latestpreNext) {
          return false;
        } else {
          return true;
        }
      }
    },
    getDrawData() {
      let _this = this;
      let j = 0;
      let barchat = {
        xAxis: [],
        series: []
      };
      let params = new URLSearchParams();
      params.append("account", _this.accountInfo.accountRS);
      params.append("firstIndex", '0');
      params.append("lastIndex", '4');
      params.append("type", "0");
      _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactions', { params }).then(res => {
        res.data.transactions.forEach(function (value, index, array) {

          if (_this.$i18n) {
            if (value.senderRS === SSO.accountRS) {
              barchat.xAxis.push(_this.$t('account.payout'));
            } else {
              barchat.xAxis.push(_this.$t('account.income'));
            }
          }
          barchat.series.push(value.amountNQT / _this.$global.unitValue);
        });
        for (; j !== 5; j++) {
          barchat.xAxis.push("");
          barchat.series.push(0);
        }

        _this.drawBarChart(barchat);
      });
    },
    getYieldData() {
      let _this = this;
      let yields = {
        xAxis: [],
        series: [],
      };
      let assets = 0;
      let params = new URLSearchParams();
      params.append("account", _this.accountInfo.accountRS);
      _this.$http.get(_this.$global.urlPrefix() + '?requestType=getBlockchainTransactions', { params }).then(res => {
        if (typeof res.data.errorDescription === "undefined") {
          let info = res.data.transactions.reverse();
          info.forEach(function (value, index, array) {
            if (value.type === 0) {
              yields.xAxis.push(_this.$global.myFormatTime(value.timestamp, "YMD", true));
              if (value.senderRS !== SSO.accountRS) {
                assets = assets + value.amountNQT / _this.$global.unitValue;
              } else {
                assets = assets - value.amountNQT / _this.$global.unitValue - value.feeNQT / _this.$global.unitValue;
              }
            } else if (value.type === 9) {
              yields.xAxis.push(_this.$global.myFormatTime(value.timestamp, "YMD", true));
              assets = assets + value.amountNQT / _this.$global.unitValue;
            } else if (value.senderRS === SSO.accountRS) {
              yields.xAxis.push(_this.$global.myFormatTime(value.timestamp, "YMD", true));
              assets = assets - value.amountNQT / _this.$global.unitValue - value.feeNQT / _this.$global.unitValue;
            }
            yields.series.push(assets);
          });
        }
        this.drawYield(yields);
      });
    },
    getTotalList: function () {
      const _this = this;
      _this.unconfirmedTransactionsList = _this.$store.state.unconfirmedTransactionsList.unconfirmedTransactions;

      let list = [];
      for (let i = 0; i < _this.unconfirmedTransactionsList.length; i++) {
        if (_this.selectType === '') {
          list.push(_this.unconfirmedTransactionsList[i]);
          _this.totalSize++;
        } else {
          if (_this.selectType === 1 && _this.unconfirmedTransactionsList[i].subtype === 0) {
            list.push(_this.unconfirmedTransactionsList[i]);
            _this.totalSize++;
          } else if (_this.selectType !== 1 && _this.selectType === _this.unconfirmedTransactionsList[i].type) {
            list.push(_this.unconfirmedTransactionsList[i]);
            _this.totalSize++;
          } else if (_this.selectType === 1.5 &&
            _this.unconfirmedTransactionsList[i].type === 1 &&
            _this.unconfirmedTransactionsList[i].subtype === 5) {
            list.push(_this.unconfirmedTransactionsList[i]);
            _this.totalSize++;
          }
        }
      }
      for (let i = 0; i < _this.accountTransactionList.length; i++) {
        list.push(_this.accountTransactionList[i]);
      }
      _this.accountTransactionList = list;

      if (_this.selectType === '') {
        // _this.getDrawData();
      }
    },
    formatRegisterStatus(status) {
      console.log('审核状态', status);
      if (status === 0) {
        return this.$t('hubsetting.register_status_invalid');
      } else if (status === 1) {
        return this.$t('hubsetting.register_status_approval');
      } else if (status === 2) {
        return this.$t('hubsetting.register_status_pending');
      }
    },
    whetherShowSendMsgBtn() {
      return true;
    },
    whetherShowStorageBtn() {
      return true;
    },
    whetherShowJoinNetBtn() {
      return false;
    },
    whetherShowOnChainBtn() {
      return false;
    },
    whetherShowHubSettingBtn() {
      /*
      At the same time satisfy the following conditions:
      1. sharder.HubBindAddress has value;
      2. using secretPhrase to login;
      3. NodeType is Hub;
      4. Hub bind MW address must equals to user account address;
      5. Not a light client;
      */
      // return true;
      return this.secretPhrase
        && !this.initHUb
        && (this.userConfig.nodeType === 'Hub' || this.userConfig.nodeType === 'Soul' || this.userConfig.nodeType === 'Center' || this.userConfig.nodeType === 'Normal')
        && this.userConfig.ssAddress === this.accountInfo.accountRS
        && !this.$global.isOpenApiProxy();
    },
    whetherShowHubInitBtn() {
      /*
      At the same time satisfy the following conditions:
      1. sharder.HubBindAddress has no value;
      2. using secretPhrase to login;
      3. NodeType is Hub;
      4. Not a light client;
      */
      // return true;
      return this.secretPhrase
        && this.initHUb
        && (this.userConfig.nodeType === 'Hub' || this.userConfig.nodeType === 'Soul' || this.userConfig.nodeType === 'Center' || this.userConfig.nodeType === 'Normal')
        && !this.$global.isOpenApiProxy();
    },
    whetherShowUseNATServiceBtn() {
      /*
      At the same time satisfy the following conditions:
      1. using secretPhrase to login；
      2. NodeType is Normal；
      3. didn't use NAT service；
      4. NAT configuration is empty;
       */
      // return this.secretPhrase
      //     && !this.userConfig.useNATService
      //     && this.userConfig.nodeType === 'Normal'
      //     && !this.userConfig.natClientSecretKey
      //     && !this.userConfig.natPort
      //     && !this.userConfig.natAddress;
      return false;
    },
    whetherShowConfigureNATServiceBtn() {
      /*
      At the same time satisfy the following conditions:
      1. using secretPhrase to login；
      2. NodeType is Normal；
      3. using NAT service；
      4. NAT configuration is not empty;
       */
      // return this.secretPhrase
      //     && this.userConfig.useNATService
      //     && this.userConfig.nodeType === 'Normal'
      //     && this.userConfig.natClientSecretKey
      //     && this.userConfig.publicAddress
      //     && this.userConfig.natPort
      //     && this.userConfig.natAddress;
      return false;
    },
    whetherShowAssetsAcrossChainsBtn() {
      return true;
    },
    getAccountRsBySecret() {
      let publicKey = global.SSO.getPublicKey(this.hubsetting.modifyMnemonicWord, false);
      let accountRs = global.SSO.getAccountIdFromPublicKey(publicKey, true);
      console.log(`accountRs: ${accountRs}`);
      return accountRs;
    },
    validationReceiver(val) {
      let _this = this;
      let receiver = _this[val].receiver;
      if (receiver === this.$global.receiverEmptyStr || receiver === this.$global.receiverPrefixStr) {
        return
      }
      if (!receiver.toUpperCase().match(_this.$global.pattern)) {
        return _this.$message.warning(_this.$t('notification.sendmessage_account_error_format'));
      }
      if (receiver === _this.accountInfo.accountRS) {
        _this.$message.warning(_this.$t('notification.account_is_self'));
        _this[val].errorCode = true;
      }
      _this[val].publicKey = "";
      _this.getAccount(receiver).then(res => {
        console.log(res);
        if (res.errorDescription || !res.publicKey) {
          _this[val].errorCode = true;
          _this[val].hasPublicKey = true;
        }
        if (res.publicKey) {
          _this[val].publicKey = res.publicKey;
        }
      });
    },

    showChains(_chainId) {
      this.chainId = _chainId;
    },

    bindAddress() {
      this.$store.state.mask = true;
      this.showChain = true;
    },

    cancel() {
      this.$store.state.mask = false;
      this.showChain = false;
    },

    bindChainAddress() {
      this.preventRepeatedClick();
      this.showChain = false;
      if (typeof (this.secretPhrase) === 'undefined') {
        this.$message.warning(this.$t("acrossChains.use_secretPhrase_tip"));
        return;
      }

      var sameAddressFlag;
      switch (this.chainId) {
        case 1:
          if (this.acrossChains.Heco.target_address == this.acrossChains.Heco.old_address) {
            sameAddressFlag = true;
          }
          break;
        case 2:
          if (this.acrossChains.OKEx.target_address == this.acrossChains.OKEx.old_address) {
            sameAddressFlag = true;
          }
          break;
        case 3:
          if (this.acrossChains.ETH.target_address == this.acrossChains.ETH.old_address) {
            sameAddressFlag = true;
          }
          break;
        case 4:
          if (this.acrossChains.Tron.target_address == this.acrossChains.Tron.old_address) {
            sameAddressFlag = true;
          }
          break;
        case 5:
          if (this.acrossChains.BSC.target_address == this.acrossChains.BSC.old_address) {
            sameAddressFlag = true;
          }
          break;
        default:
          break;
      }
      if (sameAddressFlag) {
        this.$message.warning(this.$t("acrossChains.sameAddress"));
        return;
      }

      const _this = this;
      if (
        _this.acrossChains.Heco.target_address ==
        _this.acrossChains.Heco.CosExchangeAddress &&
        _this.acrossChains.Heco.target_address ==
        _this.acrossChains.OKEx.CosExchangeAddress &&
        _this.acrossChains.Heco.target_address ==
        _this.acrossChains.ETH.CosExchangeAddress &&
        _this.acrossChains.Heco.target_address ==
        _this.acrossChains.Tron.CosExchangeAddress &&
        _this.acrossChains.Heco.target_address ==
        _this.acrossChains.BSC.CosExchangeAddress &&
        _this.acrossChains.OKEx.target_address ==
        _this.acrossChains.Heco.CosExchangeAddress &&
        _this.acrossChains.OKEx.target_address ==
        _this.acrossChains.OKEx.CosExchangeAddress &&
        _this.acrossChains.OKEx.target_address ==
        _this.acrossChains.ETH.CosExchangeAddress &&
        _this.acrossChains.OKEx.target_address ==
        _this.acrossChains.Tron.CosExchangeAddress &&
        _this.acrossChains.OKEx.target_address ==
        _this.acrossChains.BSC.CosExchangeAddress &&
        _this.acrossChains.ETH.target_address ==
        _this.acrossChains.Heco.CosExchangeAddress &&
        _this.acrossChains.ETH.target_address ==
        _this.acrossChains.OKEx.CosExchangeAddress &&
        _this.acrossChains.ETH.target_address ==
        _this.acrossChains.ETH.CosExchangeAddress &&
        _this.acrossChains.ETH.target_address ==
        _this.acrossChains.Tron.CosExchangeAddress &&
        _this.acrossChains.ETH.target_address ==
        _this.acrossChains.BSC.CosExchangeAddress &&
        _this.acrossChains.BSC.target_address ==
        _this.acrossChains.Heco.CosExchangeAddress &&
        _this.acrossChains.BSC.target_address ==
        _this.acrossChains.OKEx.CosExchangeAddress &&
        _this.acrossChains.BSC.target_address ==
        _this.acrossChains.ETH.CosExchangeAddress &&
        _this.acrossChains.BSC.target_address ==
        _this.acrossChains.Tron.CosExchangeAddress &&
        _this.acrossChains.BSC.target_address ==
        _this.acrossChains.BSC.CosExchangeAddress
      ) {
        _this.$message.warning(_this.$t("acrossChains.address_error"));
        return;
      }

      let formData = new FormData();
      formData.append("accountRS", _this.accountInfo.accountRS);
      formData.append("accountId", _this.accountInfo.accountId);
      formData.append("publicKey", _this.accountInfo.publicKey);
      switch (_this.chainId) {
        case 1:
          formData.append("Address", _this.acrossChains.Heco.target_address);
          break;
        case 2:
          formData.append("Address", _this.acrossChains.OKEx.target_address);
          break;
        case 3:
          formData.append("Address", _this.acrossChains.ETH.target_address);
          break;
        case 4:
          formData.append("Address", _this.acrossChains.Tron.target_address);
          break;
        case 5:
          formData.append("Address", _this.acrossChains.BSC.target_address);
          break;
        default:
          break;
      }
      formData.append("chainId", _this.chainId);

      this.$http.post(window.api.updateChainAccountUrl, formData)
        .then(function (res1) {
          switch (res1.data.code) {
            case "200":
              switch (_this.chainId) {
                case 1:
                  _this.acrossChains.Heco.old_address = _this.acrossChains.Heco.target_address;
                  _this.$message.success(_this.$t('acrossChains.bindAddress_success_Heco'));
                  break;
                case 2:
                  _this.acrossChains.OKEx.old_address = _this.acrossChains.OKEx.target_address;
                  _this.$message.success(_this.$t('acrossChains.bindAddress_success_OKEx'));
                  break;
                case 3:
                  _this.acrossChains.ETH.old_address = _this.acrossChains.ETH.target_address;
                  _this.$message.success(_this.$t('acrossChains.bindAddress_success_ETH'));
                  break;
                case 4:
                  _this.acrossChains.Tron.old_address = _this.acrossChains.Tron.target_address;
                  _this.$message.success(_this.$t('acrossChains.bindAddress_success_Tron'));
                  break;
                case 5:
                  _this.acrossChains.BSC.old_address = _this.acrossChains.BSC.target_address;
                  _this.$message.success(_this.$t('acrossChains.bindAddress_success_BSC'));
                  break;
                default:
                  break;
              }
              break;
            case "209":
              _this.$message.warning(_this.$t('acrossChains.bindAddress_incomplete'));
              break;
            case "202":
              _this.$message.warning(_this.$t('acrossChains.address_error'));
              break;
            case "363":
              _this.$message.warning(_this.$t('acrossChains.bindAddress_double'));
              break;
            default:
              break;
          }
        })
        .catch(err => {
          switch (_this.chainId) {
            case 1:
              _this.$message.warning(_this.$t('acrossChains.bindAddress_fail_Heco'));
              break;
            case 2:
              _this.$message.warning(_this.$t('acrossChains.bindAddress_fail_OKEx'));
              break;
            case 3:
              _this.$message.warning(_this.$t('acrossChains.bindAddress_fail_ETH'));
              break;
            case 4:
              _this.$message.warning(_this.$t('acrossChains.bindAddress_fail_Tron'));
              break;
            case 5:
              _this.$message.warning(_this.$t('acrossChains.bindAddress_fail_BSC'));
              break;
            default:
              break;
          }
        });
    }

  },
  computed: {
    getLang: function () {
      return this.$store.state.currentLang;
    },
    openAirdrop: function () {
      const _this = this;
      if (_this.hubsetting.airdropStatus && !_this.$global.isOpenApiProxy() && !_this.airdropFlag) {
        if (_this.hubsetting.airdropAccount != null) {
          _this.hubsetting.airdropAccount.forEach(ele => {
            if (ele === _this.accountInfo.accountRS) {
              _this.airdropFlag = true;
            }
          })
        }

      }
      return _this.airdropFlag;
    },
    openApiProxy: function () {
      const _this = this;
      return _this.$global.isOpenApiProxy();
    },
    nonDownloading: function () {
      return !SSO.downloadingBlockchain;
    }
  },
  watch: {
    transfer: {
      handler: function (oldValue, newValue) {
        const _this = this;
        if (!_this.transfer.hasMessage) {
          _this.transfer.message = "";
          _this.transfer.isEncrypted = false;
        }
        if (isNaN(Number(_this.transfer.number)) || _this.transfer.number < 0) {
          _this.transfer.number = 0;
        }
        if (isNaN(_this.transfer.fee) || _this.transfer.fee < 1) {
          _this.transfer.fee = 1;
        }
      },
      deep: true
    },
    messageForm: {
      handler: function (oldValue, newValue) {
        const _this = this;
        if (isNaN(_this.messageForm.fee) || _this.messageForm.fee < 1) {
          _this.messageForm.fee = 1;
        }
      },
      deep: true
    },
    selectType: function () {
      const _this = this;
      _this.currentPage = 1;
      _this.exchangeSelectType = "";
      _this.getAccountTransactionList();
    },
    exchangeSelectType: function () {
      const _this = this;
      _this.currentPage = 1;
      _this.selectType = "";
      _this.getAccountExchangeTransactionList();
    },
    getLang: {
      handler: function (oldValue, newValue) {
        console.log("语言发生变化");
        const _this = this;
        _this.transactionType = [{
          value: '',
          label: this.$t('transaction.transaction_type_all')
        }, {
          value: 0,
          label: this.$t('transaction.transaction_type_payment')
        }, {
          value: 1,
          label: this.$t('transaction.transaction_type_information')
        }, {
          value: 1.5,
          label: this.$t('transaction.transaction_type_account')
        }, {
          value: 6,
          label: this.$t('transaction.transaction_type_storage_service')
        }, {
          value: 8,
          label: this.$t('transaction.transaction_type_forge_pool')
        }, {
          value: 9,
          label: this.$t('transaction.transaction_type_system_reward')
        }, {
          value: 12,
          label: this.$t('transaction.transaction_type_poc')
        }]
      },
      deep: true
    },
    operationType(val) {
      console.log(`OperationType Changed======> ${this.operationType}`);
      if (this.operationType === 'init') {
        this.formRules = this.hubInitSettingRules;
      } else if (this.operationType === 'initNormal') {
        this.formRules = this.hubInitSettingRules;
        if (this.useNATServiceDialog && this.$refs['useNATForm']) {
          this.$refs["useNATForm"].clearValidate();
        }
      } else if (this.operationType === 'reConfig') {
        this.formRules = this.hubReconfigureSettingRules;
      } else if (this.operationType === 'reConfigNormal') {
        this.formRules = this.hubReconfigureSettingRules;
        if (this.useNATServiceDialog && this.$refs['useNATForm']) {
          this.$refs["useNATForm"].clearValidate();
        }
      } else if (this.operationType === 'reset'
        || this.operationType === 'factoryReset') {
        this.formRules = this.resettingRules;
      } else if (this.operationType === 'resetNormal') {
        this.formRules = this.resettingRules;
        if (this.useNATServiceDialog && this.$refs['useNATForm']) {
          this.$refs["useNATForm"].clearValidate();
        }
      }
      console.log(`form rules: ${JSON.stringify(this.formRules)}`)
    }
  },
  mounted() {
    const _this = this;

    let periodicTransactions = setInterval(() => {
      if (_this.$route.path === '/account') {
        _this.getAccountTransactionList();
      } else {
        clearInterval(periodicTransactions);
      }
    }, SSO.downloadingBlockchain ? this.$global.cfg.soonInterval : (this.$global.isOpenApiProxy() ? this.$global.cfg.slowInterval : this.$global.cfg.defaultInterval));

    $('#receiver').on("blur", function () {
      _this.validationReceiver("messageForm");
    });
    $('#tranfer_receiver').on("blur", function () {
      _this.validationReceiver("transfer");
    });
    $('#batch_tranfer_receiver').on("blur", function () {
      _this.validationReceiver("transfer");
    });
    window.onbeforeunload = function (e) {
      e = e || window.event;
      return e;
    };

    // this.menuAdapter()
  },
};
</script>
<style lang="scss" type="text/scss">
/*@import '~scss_vars';*/
@import "./style.scss";
@import "../../styles/css/vars.scss";

.notice-container {
  .notice {
    padding: 15px;
    margin-bottom: 20px;
    text-align: center;
    font-size: 16px;
    font-weight: 400;
    color: $primary_color;
    line-height: 150%;
    border-radius: 4px;
  }
}
</style>
<style scoped lang="scss" type="text/scss">
@import "../../styles/css/vars.scss";

@media only screen and (max-width: 780px) {
  .list_pagination /deep/ .el-pagination__jump {
    display: initial !important;
    float: right !important;
    margin-top: 11px !important;
  }

  .list_pagination
    /deep/
    .list_pagination
    .el-pagination--small
    .el-pagination__jump {
    display: initial !important;
    float: right !important;
    margin-top: 11px !important;
  }

  .exchange {
    display: none;
  }
}

.el-select-dropdown {
  .el-select-dropdown__item.selected {
    background-color: $primary_color !important;
    color: #fff !important;
  }

  .el-select-dropdown__item.selected.hover {
    background-color: $primary_color !important;
    color: #fff !important;
  }
}

.item_receiver {
  input {
    padding-left: 15px;
  }

  img {
    width: 20px;
    position: absolute;
    right: 15px;
    top: 40px;
  }
}

.calculate_fee {
  background-color: $primary_color;
  color: #fff;
  border-radius: 4px;
  border: none;
  min-width: 35px;
  padding: 0;
  font-size: 13px;
  height: 20px;
  padding: 0 5px;
}

.modal_hubSetting {
  width: 800px !important;
}

.modal_hubSetting .modal-header .modal-title {
  margin-left: 0 !important;
}

.modal_hubSetting .modal-body {
  padding: 20px 40px 60px !important;
}

.modal_hubSetting .modal-body .el-form {
  margin-top: 20px !important;
}

.modal_hubSetting .modal-body .el-form .el-form-item {
  margin-top: 18px !important;
}

/*.modal_hubSetting .modal-body .el-form .create_account .el-input{
    width:450px;
}*/
.modal_hubSetting .modal-body .el-form .create_account a {
  position: absolute;
  right: 20px;
  top: 0;
  cursor: pointer;
}

.modal_hubSetting .modal-header button {
  margin-left: 10px !important;
}

.modal_hubSetting .modal-header .long {
  width: 110px;
}
</style>
