export class Agent {
    agentId: number;
    jvmId: number;
    appName: string;
    appVersion: string;
    agentVersion: string;
    packages: string;
    excludePackages: string;
    environment: string;
    hostname: string;
    tags: string;
    methodVisibility: string;
    startedAtMillis: number;
    publishedAtMillis: number;
    pollReceivedAtMillis: number;
    nextPollExpectedAtMillis: number;
    nextPublicationExpectedAtMillis: number;
    agentAlive: boolean;
    agentLiveAndEnabled: boolean;
    deletionState: number;
    selected: boolean;
}
