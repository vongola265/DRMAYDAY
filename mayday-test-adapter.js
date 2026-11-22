const baseUrl = process.env.MAYDAY_TEST_BASE_URL;
const token = process.env.MAYDAY_TEST_ADAPTER_TOKEN;

async function invoke(operation, values = {}) {
    if (!baseUrl) throw new Error('MAYDAY_TEST_BASE_URL must point to the local Mayday test backend.');
    if (!token || token.length < 24) throw new Error('MAYDAY_TEST_ADAPTER_TOKEN must contain at least 24 characters.');
    const response = await fetch(new URL(`/_mayday_test/${operation}`, baseUrl), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Mayday-Test-Adapter': token
        },
        body: JSON.stringify(values)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(`Mayday test adapter ${operation} failed (${response.status}): ${result.message || 'unknown error'}`);
    return result;
}

module.exports = {
    assertTestDatabase: ({ expectedDatabaseName }) => invoke('assert-database', { expectedDatabaseName }),
    getRoleIds: ({ expectedDatabaseName }) => invoke('roles', { expectedDatabaseName }),
    promoteUser: ({ phone, roleId, expectedDatabaseName }) => invoke('promote-user', { phone, roleId, expectedDatabaseName }),
    deleteUser: ({ phone, expectedDatabaseName }) => invoke('delete-user', { phone, expectedDatabaseName }),
    getExpiredToken: ({ phone, expectedDatabaseName }) => invoke('expired-token', { phone, expectedDatabaseName }),
    prepareWeek7Fixtures: ({ uniquePrefix, expectedDatabaseName }) => invoke('prepare-week7', { uniquePrefix, expectedDatabaseName }),
    cleanupWeek7Fixtures: ({ fixtureIds, expectedDatabaseName }) => invoke('cleanup-week7', { fixtureIds, expectedDatabaseName }),
    prepareWeek8LesionPhoto: ({ uniquePrefix, patientId, medicalRecordId, episodeId, locationId, expectedDatabaseName }) =>
        invoke('prepare-photo', { uniquePrefix, patientId, medicalRecordId, episodeId, locationId, expectedDatabaseName }),
    cleanupWeek8Fixtures: ({ fixtureIds, expectedDatabaseName }) => invoke('cleanup-photos', { fixtureIds, expectedDatabaseName }),
    resetTestHooks: ({ expectedDatabaseName }) => invoke('reset-hooks', { expectedDatabaseName })
};
