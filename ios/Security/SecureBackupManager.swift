//
//  SecureBackupManager.swift
//  Ever Task Tools - AES-256-GCM encrypted backup manager
//
//  Swift 5.9+, iOS 16+, CryptoKit, LocalAuthentication
//

import Foundation
import CryptoKit
import LocalAuthentication
import SwiftData

// MARK: - Backup Errors
enum BackupError: Error, LocalizedError {
    case encryptionFailed
    case decryptionFailed
    case keyNotFound
    case keyGenerationFailed
    case invalidBackupData
    case fileWriteFailed
    case fileReadFailed
    case authenticationFailed
    case authenticationNotAvailable
    case migrationFailed
    
    var errorDescription: String? {
        switch self {
        case .encryptionFailed:
            return "Failed to encrypt backup data"
        case .decryptionFailed:
            return "Failed to decrypt backup data - data may be corrupted or tampered with"
        case .keyNotFound:
            return "Encryption key not found in Keychain"
        case .keyGenerationFailed:
            return "Failed to generate encryption key"
        case .invalidBackupData:
            return "Backup data is invalid or corrupted"
        case .fileWriteFailed:
            return "Failed to write backup file"
        case .fileReadFailed:
            return "Failed to read backup file"
        case .authenticationFailed:
            return "Biometric authentication failed or was cancelled"
        case .authenticationNotAvailable:
            return "Biometric authentication is not available on this device"
        case .migrationFailed:
            return "Failed to migrate existing backup to encrypted format"
        }
    }
}

// MARK: - Secure Backup Metadata
/// Metadata structure for encrypted backups
struct SecureBackupMetadata: Codable {
    let version: Int
    let createdAt: Date
    let encryptedData: Data
    
    /// Current backup format version
    static let currentVersion = 1
}

// MARK: - SecureBackupManager
/// Manages AES-256-GCM encrypted backups with biometric authentication
@MainActor
final class SecureBackupManager {
    
    // MARK: - Singleton
    
    static let shared = SecureBackupManager()
    
    // MARK: - Properties
    
    private let keyAlias = "com.evertask.backup_encryption_key"
    private let backupFileName = "secure_backup.evertask"
    private let legacyBackupFileName = "backup.json"
    private let keychainManager = KeychainManager.shared
    private let fileManager = FileManager.default
    
    /// Whether to require biometric authentication for backup/restore
    var requireBiometricAuth: Bool = false
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Public API - Backup Operations
    
    /// Exports all tasks to an encrypted backup file
    /// - Parameters:
    ///   - context: SwiftData ModelContext for fetching tasks
    ///   - requireAuth: Whether to require biometric authentication (overrides default)
    /// - Returns: URL to the encrypted backup file
    /// - Throws: BackupError if export fails
    func exportSecureBackup(
        context: ModelContext,
        requireAuth: Bool? = nil
    ) async throws -> URL {
        let shouldRequireAuth = requireAuth ?? requireBiometricAuth
        
        // Authenticate if required
        if shouldRequireAuth {
            let authenticated = try await authenticateUser(
                reason: "Authenticate to create an encrypted backup of your tasks"
            )
            guard authenticated else {
                throw BackupError.authenticationFailed
            }
        }
        
        do {
            // 1. Fetch all tasks from SwiftData
            let descriptor = FetchDescriptor<TaskItem>()
            let allTasks = try context.fetch(descriptor)
            
            // 2. Convert to backup representations
            let backupTasks = allTasks.map { $0.backupRepresentation }
            
            // 3. Encode to JSON
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.sortedKeys]
            encoder.dateEncodingStrategy = .iso8601
            let jsonData = try encoder.encode(backupTasks)
            
            // 4. Get or create encryption key
            let key = try getOrCreateEncryptionKey()
            
            // 5. Encrypt using AES-256-GCM
            let nonce = AES.GCM.Nonce()
            let sealedBox = try AES.GCM.seal(jsonData, using: key, nonce: nonce)
            
            guard let encryptedData = sealedBox.combined else {
                throw BackupError.encryptionFailed
            }
            
            // 6. Create metadata wrapper
            let metadata = SecureBackupMetadata(
                version: SecureBackupMetadata.currentVersion,
                createdAt: Date(),
                encryptedData: encryptedData
            )
            
            // 7. Encode metadata
            let backupData = try encoder.encode(metadata)
            
            // 8. Write to protected location
            let backupURL = try getSecureBackupDirectory()
                .appendingPathComponent(backupFileName)
            
            try backupData.write(
                to: backupURL,
                options: .completeFileProtectionUnlessOpen
            )
            
            // 9. Migrate/delete legacy backup if it exists
            try? removeLegacyBackup()
            
            return backupURL
            
        } catch let error as BackupError {
            throw error
        } catch {
            throw BackupError.encryptionFailed
        }
    }
    
    /// Imports tasks from an encrypted backup file
    /// - Parameters:
    ///   - url: URL to the backup file (or nil to use default location)
    ///   - context: SwiftData ModelContext for importing tasks
    ///   - mergeStrategy: How to handle conflicts with existing tasks
    ///   - requireAuth: Whether to require biometric authentication
    /// - Returns: Number of tasks restored
    /// - Throws: BackupError if import fails
    func importSecureBackup(
        from url: URL? = nil,
        context: ModelContext,
        mergeStrategy: BackupMergeStrategy = .replaceAll,
        requireAuth: Bool? = nil
    ) async throws -> Int {
        let shouldRequireAuth = requireAuth ?? requireBiometricAuth
        
        // Authenticate if required
        if shouldRequireAuth {
            let authenticated = try await authenticateUser(
                reason: "Authenticate to restore your encrypted task backup"
            )
            guard authenticated else {
                throw BackupError.authenticationFailed
            }
        }
        
        let backupURL: URL
        if let providedURL = url {
            backupURL = providedURL
        } else {
            backupURL = try getSecureBackupDirectory()
                .appendingPathComponent(backupFileName)
        }
        
        do {
            // 1. Read backup data
            let backupData = try Data(contentsOf: backupURL)
            
            // 2. Decode metadata
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let metadata = try decoder.decode(SecureBackupMetadata.self, from: backupData)
            
            // 3. Validate version
            guard metadata.version <= SecureBackupMetadata.currentVersion else {
                throw BackupError.invalidBackupData
            }
            
            // 4. Get encryption key
            let key = try getEncryptionKey()
            
            // 5. Decrypt
            let sealedBox = try AES.GCM.SealedBox(combined: metadata.encryptedData)
            let jsonData = try AES.GCM.open(sealedBox, using: key)
            
            // 6. Decode tasks
            let backups = try decoder.decode([TaskItem.BackupRepresentation].self, from: jsonData)
            
            // 7. Apply merge strategy
            let restoredCount = try applyMergeStrategy(
                backups,
                strategy: mergeStrategy,
                context: context
            )
            
            return restoredCount
            
        } catch let error as BackupError {
            throw error
        } catch {
            // Check if this might be a legacy unencrypted backup
            if let _ = try? JSONDecoder().decode([TaskItem.BackupRepresentation].self, from: Data(contentsOf: backupURL)) {
                throw BackupError.invalidBackupData
            }
            throw BackupError.decryptionFailed
        }
    }
    
    /// Attempts to restore from a legacy unencrypted backup and migrate it
    /// - Parameter context: SwiftData ModelContext
    /// - Returns: Number of tasks migrated
    /// - Throws: BackupError if migration fails
    func migrateLegacyBackup(context: ModelContext) async throws -> Int {
        let legacyURL = try getSecureBackupDirectory()
            .appendingPathComponent(legacyBackupFileName)
        
        guard fileManager.fileExists(atPath: legacyURL.path) else {
            return 0
        }
        
        do {
            // Read legacy backup
            let data = try Data(contentsOf: legacyURL)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let backups = try decoder.decode([TaskItem.BackupRepresentation].self, from: data)
            
            // Import tasks
            var migratedCount = 0
            for backup in backups {
                let task = TaskItem(from: backup)
                context.insert(task)
                migratedCount += 1
            }
            
            try context.save()
            
            // Create encrypted backup
            _ = try await exportSecureBackup(context: context)
            
            return migratedCount
            
        } catch {
            throw BackupError.migrationFailed
        }
    }
    
    /// Checks if an encrypted backup exists
    func encryptedBackupExists() -> Bool {
        guard let backupURL = try? getSecureBackupDirectory()
            .appendingPathComponent(backupFileName) else {
            return false
        }
        return fileManager.fileExists(atPath: backupURL.path)
    }
    
    /// Checks if a legacy unencrypted backup exists
    func legacyBackupExists() -> Bool {
        guard let legacyURL = try? getSecureBackupDirectory()
            .appendingPathComponent(legacyBackupFileName) else {
            return false
        }
        return fileManager.fileExists(atPath: legacyURL.path)
    }
    
    /// Returns the URL to the secure backup directory
    func getSecureBackupDirectory() throws -> URL {
        guard let documentsURL = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            throw BackupError.fileWriteFailed
        }
        
        let everTaskURL = documentsURL.appendingPathComponent("EverTask", isDirectory: true)
        
        if !fileManager.fileExists(atPath: everTaskURL.path) {
            try fileManager.createDirectory(
                at: everTaskURL,
                withIntermediateDirectories: true,
                attributes: [.protectionKey: FileProtectionType.completeUnlessOpen]
            )
        }
        
        return everTaskURL
    }
    
    /// Returns the default secure backup URL
    func getDefaultBackupURL() throws -> URL {
        return try getSecureBackupDirectory()
            .appendingPathComponent(backupFileName)
    }
    
    /// Deletes the encrypted backup
    func deleteEncryptedBackup() throws {
        let backupURL = try getSecureBackupDirectory()
            .appendingPathComponent(backupFileName)
        
        if fileManager.fileExists(atPath: backupURL.path) {
            try fileManager.removeItem(at: backupURL)
        }
    }
    
    /// Resets the encryption key (deletes and regenerates)
    /// Note: This will invalidate all existing encrypted backups
    func resetEncryptionKey() throws {
        try keychainManager.deleteKey(alias: keyAlias)
        _ = try getOrCreateEncryptionKey()
    }
    
    // MARK: - Private Methods - Key Management
    
    private func getOrCreateEncryptionKey() throws -> SymmetricKey {
        // Try to get existing key
        if let key = try? getEncryptionKey() {
            return key
        }
        
        // Generate new 256-bit key
        let key = SymmetricKey(size: .bits256)
        
        // Store in Keychain
        try keychainManager.storeSymmetricKey(key, alias: keyAlias)
        
        return key
    }
    
    private func getEncryptionKey() throws -> SymmetricKey {
        do {
            return try keychainManager.retrieveSymmetricKey(alias: keyAlias)
        } catch KeychainError.itemNotFound {
            throw BackupError.keyNotFound
        } catch {
            throw BackupError.keyNotFound
        }
    }
    
    // MARK: - Private Methods - Authentication
    
    private func authenticateUser(reason: String) async throws -> Bool {
        let context = LAContext()
        var error: NSError?
        
        // Check if biometric authentication is available
        guard context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &error
        ) else {
            // Fall back to device passcode if biometrics not available
            guard context.canEvaluatePolicy(
                .deviceOwnerAuthentication,
                error: &error
            ) else {
                throw BackupError.authenticationNotAvailable
            }
            
            // Use device passcode
            return try await context.evaluatePolicy(
                .deviceOwnerAuthentication,
                localizedReason: reason
            )
        }
        
        // Use biometric authentication
        return try await context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: reason
        )
    }
    
    // MARK: - Private Methods - Merge Strategy
    
    private func applyMergeStrategy(
        _ backups: [TaskItem.BackupRepresentation],
        strategy: BackupMergeStrategy,
        context: ModelContext
    ) throws -> Int {
        switch strategy {
        case .replaceAll:
            // Delete all existing tasks
            let descriptor = FetchDescriptor<TaskItem>()
            let existingTasks = try context.fetch(descriptor)
            for task in existingTasks {
                context.delete(task)
            }
            
            // Insert restored tasks
            for backup in backups {
                let task = TaskItem(from: backup)
                context.insert(task)
            }
            
            try context.save()
            return backups.count
            
        case .mergePreserveExisting:
            // Only insert tasks that don't exist
            var insertedCount = 0
            for backup in backups {
                let predicate = #Predicate<TaskItem> { $0.id == backup.id }
                let descriptor = FetchDescriptor<TaskItem>(predicate: predicate)
                let existing = try context.fetch(descriptor)
                
                if existing.isEmpty {
                    let task = TaskItem(from: backup)
                    context.insert(task)
                    insertedCount += 1
                }
            }
            
            try context.save()
            return insertedCount
            
        case .mergePreferBackup:
            // Replace existing tasks with backup versions
            var processedCount = 0
            for backup in backups {
                let predicate = #Predicate<TaskItem> { $0.id == backup.id }
                let descriptor = FetchDescriptor<TaskItem>(predicate: predicate)
                let existing = try context.fetch(descriptor)
                
                // Delete existing if found
                for task in existing {
                    context.delete(task)
                }
                
                // Insert from backup
                let task = TaskItem(from: backup)
                context.insert(task)
                processedCount += 1
            }
            
            try context.save()
            return processedCount
        }
    }
    
    // MARK: - Private Methods - Legacy Cleanup
    
    private func removeLegacyBackup() throws {
        let legacyURL = try getSecureBackupDirectory()
            .appendingPathComponent(legacyBackupFileName)
        
        if fileManager.fileExists(atPath: legacyURL.path) {
            try fileManager.removeItem(at: legacyURL)
        }
    }
}

// MARK: - Backup Merge Strategy
enum BackupMergeStrategy {
    /// Replace all existing tasks with backup
    case replaceAll
    /// Merge, keeping existing tasks and only adding new ones
    case mergePreserveExisting
    /// Merge, preferring backup versions over existing
    case mergePreferBackup
}
