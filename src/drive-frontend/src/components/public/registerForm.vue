<template>
    <v-main>
        <v-card style="height: 80vh; background-color: #E8F5E9" rounded="0"> 
            <v-row>
                <v-col cols="5"></v-col>
                <v-col cols="7" class="py-10 my-auto">
                    <v-card class="justify-center py-15" elevation="3" rounded="0">
                        <v-card-title class="justify-center"><span class="text-green-darken-3">Register</span></v-card-title>
                        <v-card-text>
                            <v-card-title class="justify-center">
                                <template v-if="showErrorSnack">
                                    <span class="text-red-lighten-3">{{this.validationError}}</span>
                                </template>
                            </v-card-title>
                            <v-card class="px-16" elevation="0">
                                <v-form class="px-16" ref="form" v-model="valid" lazy-validation>
                                    <v-text-field
                                        label="Email"
                                        v-model="registrationForm.email"
                                        :rules="registrationValidate.emailRules"
                                        @change="checkValidation"
                                    />
                                    <v-text-field
                                        label="Password"
                                        type="password"
                                        @click="showPasswordValidations=true"
                                        v-model="registrationForm.password"
                                        :rules="registrationValidate.passwordRules"
                                        @change="checkValidation"
                                    />
                                    <v-card-title class="justify-center">
                                       <v-btn
                                            class="success"
                                            :loading="loading"
                                            :disabled="(loading || !valid)"
                                            @click="submitRegistration"
                                            large
                                        >
                                            Register
                                        </v-btn>
                                    </v-card-title>
                                </v-form>
                            </v-card>
                            <v-card-title class="justify-center">
                                <span class="text-subtitle-2"> You already have an account?</span> 
                                <v-btn variant="plain" @click="goToLoginPage">Login</v-btn>
                            </v-card-title>
                        </v-card-text>
                    </v-card>
                </v-col>
            </v-row>
        </v-card>
    </v-main>
</template>

<script>
    export default {
        name: "RegisterForm",
        data:()=>({
            validationError: 'Email exists',
            valid: true,
            showErrorSnack: false,
            loading: false,
            passwordLengthvalid: false,
            showPasswordValidations: false,
            registrationForm:{
                email:"",
                password:"",
            },
            registrationValidate:{
                passwordRules: [
                    v => !!v || "Password is required",
                    v => (v && v.length >= 8) || "Password length must be greater than 8",
                    v => /[A-Z]/.test(v) || "Password does not contain uppercase",
                    v => /[a-z]/.test(v) || "Password does not contain lowercase",
                    v => /[0-9]/.test(v) || "Password does not contain number",
                    v => /[!@#$%^&.+-/*{}()[\]"'=]/.test(v) || "Password does not contain a special character",
                ],
                emailRules: [
                    v => !!v || 'E-mail is required',
                    //  v => /.+@.+/.test(v) || 'E-mail must be valid',
                    v =>
                        !v ||
                        /^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(v) ||
                        "E-mail must be valid",
                ],
            },
        }),
        methods: {
            submitRegistration(){
                this.loading=true;
                this.showErrorSnack=false;

                if (this.$refs.form.validate()) {
                    // send email & password to the backend
                    const requestOptions = {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(
                            { 
                                email: this.registrationForm.email,
                                password: this.registrationForm.password 
                            })
                    };
                    fetch(process.env.VUE_APP_BACKEND_URL + "/accounts", requestOptions)
                    .then(response =>{
                        this.loading=false;
                        if (!response.ok) {
                            // show error
                            if (response.status == 400) {
                                this.validationError='Email exists. Please try with another.';
                            } else {
                                this.validationError='Unknown error occurred. Please try again.';
                            }
                            this.showErrorSnack=true;
                        } else {
                            this.$router.push({ name: 'emailConfirm', params: { email: this.registrationForm.email }})
                        }
                    })
                    .catch(_ => {
                        this.loading=false;
                        this.validationError='An error occurred. Please try again.';
                        this.showErrorSnack=true;
                    })
                } else {
                    this.loading=false;
                    this.validationError='An error occurred. Please try again.';
                    this.showErrorSnack=true;
                }
            },
            goToLoginPage() {
                this.loading=true;
                this.$router.push('/login')
            },
            checkValidation() {
                this.$refs.form.validate()
            }
        },
        watch: {
            "registartionForm.password": function () {
                console.log("called here");
                if (this.registrationForm.password.length > 6) {
                    this.passwordLengthvalid = true;
                } else {
                    this.passwordLengthvalid = false;
                }
            }
        },
    };
</script>