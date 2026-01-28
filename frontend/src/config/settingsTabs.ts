import { h, type Component } from 'vue'
import { StoreService } from '@/services/StoreService'
import { UserService } from '@/services/UserService'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { JobService } from '@/services/JobService'
import { queryKeys } from '@/composables/queries/queryKeys' 
import { ERootCategoryList } from '@/enums'

// Components
import StoreForm from '@/components/forms/StoreForm.vue'
import UserForm from '@/components/forms/UserForm.vue'
import AttributeTypeForm from '@/components/forms/AttributeTypeForm.vue'
import AttributeTypeEditForm from '@/components/forms/AttributeTypeEditForm.vue'
import CategoryBatchImport from '@/components/forms/CategoryBatchImport.vue'
import JobDetailPane from '@/components/panes/JobDetailPane.vue'
import StandardDetailPane from '@/components/panes/StandardDetailPane.vue'
import JobTriggerMenu from '@/components/widgets/JobTriggerMenu.vue'
import JobFilter from '@/components/widgets/JobFilter.vue'

// UI Cells
import BadgeCell from '@/components/ui/cells/BadgeCell.vue'
import DateCell from '@/components/ui/cells/DateCell.vue'
import SimpleCell from '@/components/ui/cells/SimpleCell.vue'

const getCategoryStaticData = () => {
    return ERootCategoryList.map(key => {
        const label = key.charAt(0).toUpperCase() + key.slice(1).toLowerCase();
        return { uuid: key, key: key, name: label, icon: 'Folder' }
    })
}

export interface TabConfig {
    title: string;
    queryResolver?: (filterValue: any) => { queryKey: readonly unknown[], queryFn: () => Promise<any> };
    query?: { queryKey: readonly unknown[], queryFn: () => Promise<any> };
    staticData?: any[]; 
    service?: any;
    columns: any[];
    components: { 
        create?: Component; 
        edit?: Component; 
        detail: Component; 
        toolbar?: Component;
        actions?: Component;
    };
}

const jobColumns = [
    { 
        accessorKey: 'taskName', 
        header: 'Task Name', 
        size: 180, 
        cell: (i: any) => h(SimpleCell, { value: i.getValue(), bold: true }) 
    },
    { 
        accessorKey: 'status', 
        header: 'Status', 
        size: 90, 
        cell: (i: any) => h(BadgeCell, { 
            value: i.getValue(),
            colors: {
                'COMPLETED': 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/30 border-green-200 dark:border-green-800',
                'FAILED': 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/30 border-red-200 dark:border-red-800',
                'RUNNING': 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800 animate-pulse',
                'QUEUED': 'text-orange-600 dark:text-orange-400 bg-orange-50 dark:bg-orange-900/30 border-orange-200 dark:border-orange-800'
            }
        })
    },
    { 
        accessorKey: 'executionTime', 
        header: 'Run Time', 
        size: 140, 
        cell: (i: any) => h(DateCell, { value: i.getValue() }) 
    },
    { 
        accessorKey: 'instanceId', 
        header: 'ID', 
        size: 100, 
        cell: (i: any) => h(SimpleCell, { value: i.getValue(), mono: true, class: 'text-gray-400' }) 
    }
];

export const SETTINGS_TABS: Record<string, TabConfig> = {
    stores: {
        title: 'Physical Stores',
        // DIRECT SERVICE CALL
        query: { queryKey: queryKeys.admin.stores, queryFn: () => StoreService.getAll() }, 
        service: StoreService,
        components: { create: StoreForm, edit: StoreForm, detail: StandardDetailPane },
        columns: [
            { accessorKey: 'name', header: 'Store Name', size: 180, cell: (i: any) => h(SimpleCell, { value: i.getValue() }) },
            { accessorKey: 'shortname', header: 'Code', size: 80, cell: (i: any) => h(BadgeCell, { value: i.getValue(), defaultColor: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800' }) }
        ]
    },
    users: {
        title: 'System Users',
        query: { queryKey: queryKeys.admin.users, queryFn: () => UserService.getAll() },
        service: UserService,
        components: { create: UserForm, edit: UserForm, detail: StandardDetailPane },
        columns: [
            { accessorKey: 'name', header: 'Username', size: 120, cell: (i: any) => h(SimpleCell, { value: i.getValue(), bold: true }) },
            { accessorKey: 'email', header: 'Email', size: 180, cell: (i: any) => h(SimpleCell, { value: i.getValue(), class: 'text-gray-500' }) }
        ]
    },
    attributes: {
        title: 'Attribute Definitions',
        query: { queryKey: queryKeys.admin.attributes, queryFn: () => AttributeTypeService.getAll() },
        service: AttributeTypeService,
        components: { create: AttributeTypeForm, edit: AttributeTypeEditForm, detail: StandardDetailPane },
        columns: [
            { accessorKey: 'name', header: 'Attribute Name', size: 160, cell: (i: any) => h(SimpleCell, { value: i.getValue(), bold: true }) },
            { accessorKey: 'dataType', header: 'Type', size: 80, cell: (i: any) => h(BadgeCell, { value: i.getValue(), defaultColor: 'text-purple-700 dark:text-purple-400 bg-purple-50 dark:bg-purple-900/30 border-purple-100 dark:border-purple-800' }) }
        ]
    },
    jobs: {
        title: 'System Jobs',
        // DIRECT SERVICE CALL (Uses JobService)
        queryResolver: (statusFilter) => ({ 
            queryKey: queryKeys.admin.jobs(statusFilter), 
            queryFn: () => JobService.getUnifiedJobs()
        }),
        service: {}, 
        components: { 
            detail: JobDetailPane,
            toolbar: JobFilter,
            actions: JobTriggerMenu
        },
        columns: jobColumns
    },
    import: {
        title: 'Import Target',
        staticData: getCategoryStaticData(),
        components: { detail: CategoryBatchImport },
        columns: [
            { accessorKey: 'name', header: 'Dimension', size: 150, cell: (i: any) => h(SimpleCell, { value: i.getValue(), bold: true }) },
            { accessorKey: 'key', header: 'ID', size: 60, cell: (i: any) => h(SimpleCell, { value: i.getValue(), mono: true }) }
        ]
    }
}