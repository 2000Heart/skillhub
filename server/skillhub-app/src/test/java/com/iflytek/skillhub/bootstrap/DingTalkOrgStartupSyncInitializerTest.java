package com.iflytek.skillhub.bootstrap;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iflytek.skillhub.config.DingTalkProperties;
import com.iflytek.skillhub.service.DingTalkOrgSyncService;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class DingTalkOrgStartupSyncInitializerTest {

    @Mock
    private DingTalkOrgSyncService dingTalkOrgSyncService;

    @Mock
    private Executor backgroundExecutor;

    private DingTalkProperties dingTalkProperties;
    private DingTalkOrgStartupSyncInitializer initializer;

    @BeforeEach
    void setUp() {
        dingTalkProperties = new DingTalkProperties();
        dingTalkProperties.getOrgSync().setStartupEnabled(true);
        initializer = new DingTalkOrgStartupSyncInitializer(
                dingTalkProperties,
                dingTalkOrgSyncService,
                backgroundExecutor
        );
    }

    @Test
    void run_schedulesSyncWhenInitialBaselineMissing() {
        when(dingTalkOrgSyncService.needsInitialFullSync()).thenReturn(true);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(backgroundExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    @Test
    void run_skipsWhenBaselineAlreadyExists() {
        when(dingTalkOrgSyncService.needsInitialFullSync()).thenReturn(false);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(backgroundExecutor, never()).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    @Test
    void run_skipsWhenStartupSyncDisabled() {
        dingTalkProperties.getOrgSync().setStartupEnabled(false);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(dingTalkOrgSyncService, never()).needsInitialFullSync();
        verify(backgroundExecutor, never()).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }
}
