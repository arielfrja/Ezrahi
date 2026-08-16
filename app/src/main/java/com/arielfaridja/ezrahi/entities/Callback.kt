package com.arielfaridja.ezrahi.entities

interface Callback<T> {
    fun onResponse(response: Response<T>)

    class Response<T> {
        var users: HashMap<String, ActUser>? = null
        var activities: HashMap<String, Activity>? = null
        var activitiesList: ArrayList<Activity>? = null
        var message: String? = null
        var user: User? = null
        var activity: Activity? = null
        var exception: Exception? = null
        var data: T? = null

        constructor(activities: ArrayList<Activity>?) {
            this.activitiesList = activities
        }

        constructor()

        constructor(items: HashMap<String, T>?) {
            data = null
            activities = HashMap()
            users = HashMap()
            items?.forEach { (s, o) ->
                when (o) {
                    is ActUser -> users?.put(s, o)
                    is Activity -> activities?.put(s, o)
                }
            }
        }

        constructor(message: String?) {
            this.message = message
        }

        constructor(user: User?) {
            this.user = user
        }

        constructor(activity: Activity?) {
            this.activity = activity
        }

        constructor(exception: Exception?) {
            this.exception = exception
        }

        constructor(data: T?) {
            this.data = data
        }
    }
}
