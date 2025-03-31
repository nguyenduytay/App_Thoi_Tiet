package com.example.weather2.Repository

import androidx.lifecycle.LiveData
import com.example.weather2.Model.Dao.WarningDao
import com.example.weather2.Model.Entity.Timer
import com.example.weather2.Model.Entity.Warning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow

class WarningRepository(private val warningDao: WarningDao) {

    suspend fun getAllWarnings(): List<Warning> {
        return warningDao.getAllWarnings()
    }

    suspend fun insert(warning: Warning) {
        warningDao.insertWarning(warning)
    }

    suspend fun delete(id: Int) {
        warningDao.deleteWarning(id)
    }
    suspend fun updateWarning(warning : Warning)
    {
        warningDao.updateWarning(warning)
    }

     fun getWarning(id : Int) : Flow<Warning>
    {
        return warningDao.getWarning(id)
    }
    private fun uploadWarningsToFirebase(warning: Warning) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user"
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$userId/warnings")
        dbRef.setValue(warning)
    }

}

