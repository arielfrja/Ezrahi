package com.arielfaridja.ezrahi.UI.Signup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import com.arielfaridja.ezrahi.UI.Login.LoginActivity
import com.arielfaridja.ezrahi.UI.Signup.ui.theme.EzrahiTheme

class SignupActivity : ComponentActivity() {
    private lateinit var model: SignupActivityViewModel
    private var spacingModifier = Modifier.padding(4.dp)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this).get(SignupActivityViewModel::class.java)
        //TODO(getString(R.string.set_post_signed_up_action))
        setContent {
            EzrahiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LoginForm()
                }
            }
        }
    }

    @Composable
    fun LoginForm() {
        val enable = remember { mutableStateOf(true) }
        val firstNameState = remember { mutableStateOf("") }
        val lastNameState = remember { mutableStateOf("") }
        val emailState = remember { mutableStateOf("") }
        val phoneState = remember { mutableStateOf("") }
        val passwordState = remember { mutableStateOf("") }
        val passwordVerifState = remember { mutableStateOf("") }

        val firstNameErrorState = remember { mutableStateOf("") }
        val lastNameErrorState = remember { mutableStateOf("") }
        val emailErrorState = remember { mutableStateOf("") }
        val phoneErrorState = remember { mutableStateOf("") }
        val passwordErrorState = remember { mutableStateOf("") }
        val passwordVerifErrorState = remember { mutableStateOf("") }
        val dialogMessageState = remember { mutableStateOf("") }
        val signedUpState = remember { mutableStateOf(false) }

        model.exception.observe(this) { e ->
            if (e != null)
                dialogMessageState.value = e.message!!
        }
        model.signedUp.observe(this) {
            if (it)
                signedUpState.value = true
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(36.dp)
                .border(0.dp, Color.White)
                .fillMaxWidth(1f)
        ) {
            Text(text = "Sign up", style = MaterialTheme.typography.h3)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(1f)
            )
            {
                //first Name Text Field
                Column(modifier = Modifier.weight(1f))
                {
                    TextFieldCompose(
                        label = "First Name",
                        state = firstNameState,
                        errorState = firstNameErrorState,
                        modelField = Fields.FIRST_NAME,
                    )
                }

                //last Name Text Field
                Column(modifier = Modifier.weight(1f))
                {
                    TextFieldCompose(
                        label = "Last Name",
                        state = lastNameState,
                        errorState = lastNameErrorState,
                        modelField = Fields.LAST_NAME
                    )
                }
            }

            TextFieldCompose("Email", emailState, emailErrorState, Fields.EMAIL)

            TextFieldCompose(
                label = "Phone",
                state = phoneState,
                errorState = phoneErrorState,
                modelField = Fields.PHONE
            )

            TextFieldCompose(
                label = "Password",
                state = passwordState,
                errorState = passwordErrorState,
                modelField = Fields.PASSWORD
            )

            TextFieldCompose(
                label = "retype password here",
                state = passwordVerifState,
                errorState = passwordVerifErrorState,
                modelField = Fields.PASSWORD_VERIFICATION
            )

            Button(
                onClick = {
                    //TODO: verificate fields, call to db.
                    try {
                        enable.value = false
                        model.signupEmailPassword()
                    } catch (e: Exception) {
                        enable.value = true
                        val message = e.message
                        if (message != null) {
                            if (message.startsWith("F: ")) {
                                if (message.contains(SignupActivityViewModel.EMAIL_EMPTY))
                                    emailErrorState.value = SignupActivityViewModel.EMAIL_EMPTY
                                else if (message.contains(SignupActivityViewModel.EMAIL_NOT_VALID))
                                    emailErrorState.value = SignupActivityViewModel.EMAIL_NOT_VALID

                                if (message.contains(SignupActivityViewModel.PASSWORD_EMPTY))
                                    passwordErrorState.value =
                                        SignupActivityViewModel.PASSWORD_EMPTY
                                else if (message.contains(SignupActivityViewModel.PASSWORD_SHORT))
                                    passwordErrorState.value =
                                        SignupActivityViewModel.PASSWORD_SHORT
                                else if (message.contains(SignupActivityViewModel.PASSWORD_DIFFERENT)) {
                                    passwordErrorState.value =
                                        SignupActivityViewModel.PASSWORD_DIFFERENT
                                    passwordVerifErrorState.value =
                                        SignupActivityViewModel.PASSWORD_DIFFERENT
                                }

                                if (message.contains(SignupActivityViewModel.FIRST_NAME_EMPTY))
                                    firstNameErrorState.value =
                                        SignupActivityViewModel.FIRST_NAME_EMPTY
                                if (message.contains(SignupActivityViewModel.LAST_NAME_EMPTY))
                                    lastNameErrorState.value =
                                        SignupActivityViewModel.LAST_NAME_EMPTY
                                if (message.contains(SignupActivityViewModel.PHONE_EMPTY))
                                    phoneErrorState.value = SignupActivityViewModel.PHONE_EMPTY

                            } else
                                dialogMessageState.value = message
                        }
                    }
                },
                modifier = spacingModifier, enabled = enable.value
            ) {
                Text(text = "sign up")
            }
        }

        AnimatedVisibility(
            visible = !enable.value, modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCCFFFFFF)),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 0.dp, Color(0x00000000))
            ) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        if (dialogMessageState.value.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { dialogMessageState.value = "" },
                buttons = {
                    Box(Modifier.padding(4.dp)) {
                        Button(
                            onClick = {
                                dialogMessageState.value = ""
                                model.setTheException(null)
                                enable.value = true
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                },
                title = { Text("Error") },
                text = { Text(text = dialogMessageState.value) },
                properties = DialogProperties()
            )
        }

        if (signedUpState.value) {
            AlertDialog(onDismissRequest = { goToLoginActivity() },
                properties = DialogProperties(),
                title = {
                    Text(text = "Congratulation!", style = MaterialTheme.typography.h6)
                },
                text = { Text(text = "account create successfully, verify your email, than login from login screen") },
                buttons = {
                    Box(modifier = Modifier.padding(4.dp))
                    {
                        Button(onClick = { goToLoginActivity() })
                        { Text("bring me there") }
                    }
                })

        }
    }

    private fun goToLoginActivity() {
        intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    @Composable
    private fun TextFieldCompose(
        label: String,
        state: MutableState<String>,
        errorState: MutableState<String>,
        modelField: Fields
    ) {
        Column {
            TextField(
                value = state.value,
                isError = errorState.value.isNotEmpty(),
                onValueChange = {
                    when (modelField) {
                        Fields.FIRST_NAME -> model.firstName = it.lowercase()
                        Fields.LAST_NAME -> model.lastName = it.lowercase()
                        Fields.EMAIL -> model.email = it.lowercase()
                        Fields.PHONE -> model.phone = it.lowercase()
                        Fields.PASSWORD -> model.password = it.lowercase()
                        Fields.PASSWORD_VERIFICATION -> model.passwordVerification = it.lowercase()
                    }

                    state.value = it
                    if (errorState.value.isNotEmpty())
                        errorState.value = ""
                },
                label = { Text(label) },
                modifier = spacingModifier.fillMaxWidth(1f),
                trailingIcon = {
                    if (errorState.value.isNotEmpty())
                        Icon(Icons.Filled.Error, "error")

                },
                visualTransformation = when (modelField) {
                    Fields.PASSWORD, Fields.PASSWORD_VERIFICATION -> PasswordVisualTransformation()
                    else -> VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (modelField) {
                        Fields.EMAIL -> KeyboardType.Email
                        Fields.PHONE -> KeyboardType.Phone
                        Fields.PASSWORD -> KeyboardType.Password
                        Fields.PASSWORD_VERIFICATION -> KeyboardType.Password
                        else -> KeyboardType.Text
                    },
                    capitalization = when (modelField) {
                        Fields.FIRST_NAME, Fields.LAST_NAME -> KeyboardCapitalization.Words
                        else -> KeyboardCapitalization.None
                    },
                    imeAction = when (modelField) {
                        Fields.PASSWORD_VERIFICATION -> ImeAction.Done
                        else -> ImeAction.Next
                    }
                )
            )
            //if (errorState.value.isNotEmpty())
            AnimatedVisibility(
                visible = errorState.value.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {


                Text(
                    text = errorState.value, fontSize = 12.sp,
                    modifier = Modifier.Companion
                        .align(Start)
                        .padding(4.dp, 0.dp),

                    style = TextStyle(color = Color(0xFFB00020))//code number of error color (MaterialTheme.colors.error not working well
                )
            }

        }

    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        EzrahiTheme {
            LoginForm()
        }

    }


    enum class Fields {
        FIRST_NAME,
        LAST_NAME,
        EMAIL,
        PHONE,
        PASSWORD,
        PASSWORD_VERIFICATION
    }

}