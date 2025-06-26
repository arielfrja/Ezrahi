package com.arielfaridja.ezrahi.entities

class Activity() {
    var id: String = ""
    var name: String = ""
    var routesSrc: ArrayList<String> = ArrayList()
    var owner: User = User()
    var users: ArrayList<ActUser> = ArrayList()
    var permissions: ArrayList<ActPermission> = ArrayList()

    constructor(
        id: String,
        name: String,
        routesSrc: ArrayList<String>,
        owner: User,
        users: ArrayList<ActUser>,
        permissions: ArrayList<ActPermission>
    ) : this() {
        this.id = id
        this.name = name
        this.routesSrc = routesSrc
        this.owner = owner
        this.users = users
        this.permissions = permissions
    }
}
