package com.example.data.repository

import com.example.data.database.MacroDao
import com.example.data.database.MacroEntity
import com.example.data.database.TemplateDao
import com.example.data.database.TemplateEntity
import com.example.model.AutomationStep
import com.example.model.MacroScript
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutomationRepository(
    private val macroDao: MacroDao,
    private val templateDao: TemplateDao
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stepsType = Types.newParameterizedType(List::class.java, AutomationStep::class.java)
    private val adapter = moshi.adapter<List<AutomationStep>>(stepsType)

    // Flow of all macro scripts mapped cleanly
    val allScripts: Flow<List<MacroScript>> = macroDao.getAllMacros().map { entities ->
        entities.map { entity ->
            val steps = try {
                adapter.fromJson(entity.stepsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            MacroScript(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                steps = steps,
                createdAt = entity.createdAt
            )
        }
    }

    // Flow of all template images
    val allTemplates: Flow<List<TemplateEntity>> = templateDao.getAllTemplates()

    // Insert or update macro
    suspend fun saveMacro(script: MacroScript) {
        val json = adapter.toJson(script.steps)
        val entity = MacroEntity(
            id = script.id,
            name = script.name,
            description = script.description,
            stepsJson = json,
            createdAt = script.createdAt
        )
        macroDao.insertMacro(entity)
    }

    // Delete macro
    suspend fun deleteMacro(id: Int) {
        macroDao.deleteMacroById(id)
    }

    // Insert template icon
    suspend fun saveTemplate(template: TemplateEntity) {
        templateDao.insertTemplate(template)
    }

    // Delete template icon
    suspend fun deleteTemplateById(id: Int) {
        templateDao.deleteTemplateById(id)
    }

    suspend fun deleteTemplateByName(name: String) {
        templateDao.deleteTemplateByName(name)
    }
}
