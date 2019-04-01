package com.cnr_isac.oldmusa.api


class Channel(
    api: Api,
    id: Long,
    val sensorId: Long,
    val cnrId: Long?,
    var name: String?,
    var measureUnit: String?,
    var rangeMin: Double?,
    var rangeMax: Double?
) : ApiEntity(api, id) {

    fun onUpdate(data: ApiChannel) {
        assert(id == data.id)
        assert(cnrId == data.cnrId)
        assert(sensorId == data.sensorId)
        this.name = data.name
        this.measureUnit = data.measureUnit
        this.rangeMin = data.rangeMin
        this.rangeMax = data.rangeMax
    }

    fun serialize(): ApiChannel {
        return ApiChannel(id, sensorId, cnrId, name, measureUnit, rangeMin, rangeMax)
    }

    fun commit() {
        api.updateChannel(id, serialize())
    }

    fun delete() {
        api.deleteChannel(id)
    }
}