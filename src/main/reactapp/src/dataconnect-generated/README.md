# Generated TypeScript README
This README will guide you through the process of using the generated JavaScript SDK package for the connector `example`. It will also provide examples on how to use your generated SDK to call your Data Connect queries and mutations.

**If you're looking for the `React README`, you can find it at [`dataconnect-generated/react/README.md`](./react/README.md)**

***NOTE:** This README is generated alongside the generated SDK. If you make changes to this file, they will be overwritten when the SDK is regenerated.*

# Table of Contents
- [**Overview**](#generated-javascript-readme)
- [**Accessing the connector**](#accessing-the-connector)
  - [*Connecting to the local Emulator*](#connecting-to-the-local-emulator)
- [**Queries**](#queries)
  - [*GetPageContentByPath*](#getpagecontentbypath)
- [**Mutations**](#mutations)
  - [*InsertPageContent*](#insertpagecontent)
  - [*UpdatePageContent*](#updatepagecontent)
  - [*DeletePageContent*](#deletepagecontent)

# Accessing the connector
A connector is a collection of Queries and Mutations. One SDK is generated for each connector - this SDK is generated for the connector `example`. You can find more information about connectors in the [Data Connect documentation](https://firebase.google.com/docs/data-connect#how-does).

You can use this generated SDK by importing from the package `@dataconnect/generated` as shown below. Both CommonJS and ESM imports are supported.

You can also follow the instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#set-client).

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
```

## Connecting to the local Emulator
By default, the connector will connect to the production service.

To connect to the emulator, you can use the following code.
You can also follow the emulator instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#instrument-clients).

```typescript
import { connectDataConnectEmulator, getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
connectDataConnectEmulator(dataConnect, 'localhost', 9399);
```

After it's initialized, you can call your Data Connect [queries](#queries) and [mutations](#mutations) from your generated SDK.

# Queries

There are two ways to execute a Data Connect Query using the generated Web SDK:
- Using a Query Reference function, which returns a `QueryRef`
  - The `QueryRef` can be used as an argument to `executeQuery()`, which will execute the Query and return a `QueryPromise`
- Using an action shortcut function, which returns a `QueryPromise`
  - Calling the action shortcut function will execute the Query and return a `QueryPromise`

The following is true for both the action shortcut function and the `QueryRef` function:
- The `QueryPromise` returned will resolve to the result of the Query once it has finished executing
- If the Query accepts arguments, both the action shortcut function and the `QueryRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Query
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each query. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-queries).

## GetPageContentByPath
You can execute the `GetPageContentByPath` query using the following action shortcut function, or by calling `executeQuery()` after calling the following `QueryRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
getPageContentByPath(vars: GetPageContentByPathVariables): QueryPromise<GetPageContentByPathData, GetPageContentByPathVariables>;

interface GetPageContentByPathRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: GetPageContentByPathVariables): QueryRef<GetPageContentByPathData, GetPageContentByPathVariables>;
}
export const getPageContentByPathRef: GetPageContentByPathRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `QueryRef` function.
```typescript
getPageContentByPath(dc: DataConnect, vars: GetPageContentByPathVariables): QueryPromise<GetPageContentByPathData, GetPageContentByPathVariables>;

interface GetPageContentByPathRef {
  ...
  (dc: DataConnect, vars: GetPageContentByPathVariables): QueryRef<GetPageContentByPathData, GetPageContentByPathVariables>;
}
export const getPageContentByPathRef: GetPageContentByPathRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the getPageContentByPathRef:
```typescript
const name = getPageContentByPathRef.operationName;
console.log(name);
```

### Variables
The `GetPageContentByPath` query requires an argument of type `GetPageContentByPathVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface GetPageContentByPathVariables {
  path: string;
}
```
### Return Type
Recall that executing the `GetPageContentByPath` query returns a `QueryPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `GetPageContentByPathData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface GetPageContentByPathData {
  pageContents: ({
    id: UUIDString;
    contentHtml: string;
    lastUpdated: TimestampString;
    metaDescription?: string | null;
    pageTitle?: string | null;
    path: string;
    scriptsJs?: string | null;
    stylesCss?: string | null;
  } & PageContent_Key)[];
}
```
### Using `GetPageContentByPath`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, getPageContentByPath, GetPageContentByPathVariables } from '@dataconnect/generated';

// The `GetPageContentByPath` query requires an argument of type `GetPageContentByPathVariables`:
const getPageContentByPathVars: GetPageContentByPathVariables = {
  path: ..., 
};

// Call the `getPageContentByPath()` function to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await getPageContentByPath(getPageContentByPathVars);
// Variables can be defined inline as well.
const { data } = await getPageContentByPath({ path: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await getPageContentByPath(dataConnect, getPageContentByPathVars);

console.log(data.pageContents);

// Or, you can use the `Promise` API.
getPageContentByPath(getPageContentByPathVars).then((response) => {
  const data = response.data;
  console.log(data.pageContents);
});
```

### Using `GetPageContentByPath`'s `QueryRef` function

```typescript
import { getDataConnect, executeQuery } from 'firebase/data-connect';
import { connectorConfig, getPageContentByPathRef, GetPageContentByPathVariables } from '@dataconnect/generated';

// The `GetPageContentByPath` query requires an argument of type `GetPageContentByPathVariables`:
const getPageContentByPathVars: GetPageContentByPathVariables = {
  path: ..., 
};

// Call the `getPageContentByPathRef()` function to get a reference to the query.
const ref = getPageContentByPathRef(getPageContentByPathVars);
// Variables can be defined inline as well.
const ref = getPageContentByPathRef({ path: ..., });

// You can also pass in a `DataConnect` instance to the `QueryRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = getPageContentByPathRef(dataConnect, getPageContentByPathVars);

// Call `executeQuery()` on the reference to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeQuery(ref);

console.log(data.pageContents);

// Or, you can use the `Promise` API.
executeQuery(ref).then((response) => {
  const data = response.data;
  console.log(data.pageContents);
});
```

# Mutations

There are two ways to execute a Data Connect Mutation using the generated Web SDK:
- Using a Mutation Reference function, which returns a `MutationRef`
  - The `MutationRef` can be used as an argument to `executeMutation()`, which will execute the Mutation and return a `MutationPromise`
- Using an action shortcut function, which returns a `MutationPromise`
  - Calling the action shortcut function will execute the Mutation and return a `MutationPromise`

The following is true for both the action shortcut function and the `MutationRef` function:
- The `MutationPromise` returned will resolve to the result of the Mutation once it has finished executing
- If the Mutation accepts arguments, both the action shortcut function and the `MutationRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Mutation
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each mutation. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-mutations).

## InsertPageContent
You can execute the `InsertPageContent` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
insertPageContent(vars: InsertPageContentVariables): MutationPromise<InsertPageContentData, InsertPageContentVariables>;

interface InsertPageContentRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: InsertPageContentVariables): MutationRef<InsertPageContentData, InsertPageContentVariables>;
}
export const insertPageContentRef: InsertPageContentRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
insertPageContent(dc: DataConnect, vars: InsertPageContentVariables): MutationPromise<InsertPageContentData, InsertPageContentVariables>;

interface InsertPageContentRef {
  ...
  (dc: DataConnect, vars: InsertPageContentVariables): MutationRef<InsertPageContentData, InsertPageContentVariables>;
}
export const insertPageContentRef: InsertPageContentRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the insertPageContentRef:
```typescript
const name = insertPageContentRef.operationName;
console.log(name);
```

### Variables
The `InsertPageContent` mutation requires an argument of type `InsertPageContentVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface InsertPageContentVariables {
  contentHtml: string;
  path: string;
  lastUpdated: TimestampString;
  metaDescription?: string | null;
  pageTitle?: string | null;
  scriptsJs?: string | null;
  stylesCss?: string | null;
}
```
### Return Type
Recall that executing the `InsertPageContent` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `InsertPageContentData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface InsertPageContentData {
  pageContent_insert: PageContent_Key;
}
```
### Using `InsertPageContent`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, insertPageContent, InsertPageContentVariables } from '@dataconnect/generated';

// The `InsertPageContent` mutation requires an argument of type `InsertPageContentVariables`:
const insertPageContentVars: InsertPageContentVariables = {
  contentHtml: ..., 
  path: ..., 
  lastUpdated: ..., 
  metaDescription: ..., // optional
  pageTitle: ..., // optional
  scriptsJs: ..., // optional
  stylesCss: ..., // optional
};

// Call the `insertPageContent()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await insertPageContent(insertPageContentVars);
// Variables can be defined inline as well.
const { data } = await insertPageContent({ contentHtml: ..., path: ..., lastUpdated: ..., metaDescription: ..., pageTitle: ..., scriptsJs: ..., stylesCss: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await insertPageContent(dataConnect, insertPageContentVars);

console.log(data.pageContent_insert);

// Or, you can use the `Promise` API.
insertPageContent(insertPageContentVars).then((response) => {
  const data = response.data;
  console.log(data.pageContent_insert);
});
```

### Using `InsertPageContent`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, insertPageContentRef, InsertPageContentVariables } from '@dataconnect/generated';

// The `InsertPageContent` mutation requires an argument of type `InsertPageContentVariables`:
const insertPageContentVars: InsertPageContentVariables = {
  contentHtml: ..., 
  path: ..., 
  lastUpdated: ..., 
  metaDescription: ..., // optional
  pageTitle: ..., // optional
  scriptsJs: ..., // optional
  stylesCss: ..., // optional
};

// Call the `insertPageContentRef()` function to get a reference to the mutation.
const ref = insertPageContentRef(insertPageContentVars);
// Variables can be defined inline as well.
const ref = insertPageContentRef({ contentHtml: ..., path: ..., lastUpdated: ..., metaDescription: ..., pageTitle: ..., scriptsJs: ..., stylesCss: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = insertPageContentRef(dataConnect, insertPageContentVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.pageContent_insert);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.pageContent_insert);
});
```

## UpdatePageContent
You can execute the `UpdatePageContent` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
updatePageContent(vars: UpdatePageContentVariables): MutationPromise<UpdatePageContentData, UpdatePageContentVariables>;

interface UpdatePageContentRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpdatePageContentVariables): MutationRef<UpdatePageContentData, UpdatePageContentVariables>;
}
export const updatePageContentRef: UpdatePageContentRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
updatePageContent(dc: DataConnect, vars: UpdatePageContentVariables): MutationPromise<UpdatePageContentData, UpdatePageContentVariables>;

interface UpdatePageContentRef {
  ...
  (dc: DataConnect, vars: UpdatePageContentVariables): MutationRef<UpdatePageContentData, UpdatePageContentVariables>;
}
export const updatePageContentRef: UpdatePageContentRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the updatePageContentRef:
```typescript
const name = updatePageContentRef.operationName;
console.log(name);
```

### Variables
The `UpdatePageContent` mutation requires an argument of type `UpdatePageContentVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface UpdatePageContentVariables {
  id: UUIDString;
  contentHtml?: string | null;
  lastUpdated?: TimestampString | null;
  metaDescription?: string | null;
  pageTitle?: string | null;
  path?: string | null;
  scriptsJs?: string | null;
  stylesCss?: string | null;
}
```
### Return Type
Recall that executing the `UpdatePageContent` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `UpdatePageContentData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface UpdatePageContentData {
  pageContent_update?: PageContent_Key | null;
}
```
### Using `UpdatePageContent`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, updatePageContent, UpdatePageContentVariables } from '@dataconnect/generated';

// The `UpdatePageContent` mutation requires an argument of type `UpdatePageContentVariables`:
const updatePageContentVars: UpdatePageContentVariables = {
  id: ..., 
  contentHtml: ..., // optional
  lastUpdated: ..., // optional
  metaDescription: ..., // optional
  pageTitle: ..., // optional
  path: ..., // optional
  scriptsJs: ..., // optional
  stylesCss: ..., // optional
};

// Call the `updatePageContent()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await updatePageContent(updatePageContentVars);
// Variables can be defined inline as well.
const { data } = await updatePageContent({ id: ..., contentHtml: ..., lastUpdated: ..., metaDescription: ..., pageTitle: ..., path: ..., scriptsJs: ..., stylesCss: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await updatePageContent(dataConnect, updatePageContentVars);

console.log(data.pageContent_update);

// Or, you can use the `Promise` API.
updatePageContent(updatePageContentVars).then((response) => {
  const data = response.data;
  console.log(data.pageContent_update);
});
```

### Using `UpdatePageContent`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, updatePageContentRef, UpdatePageContentVariables } from '@dataconnect/generated';

// The `UpdatePageContent` mutation requires an argument of type `UpdatePageContentVariables`:
const updatePageContentVars: UpdatePageContentVariables = {
  id: ..., 
  contentHtml: ..., // optional
  lastUpdated: ..., // optional
  metaDescription: ..., // optional
  pageTitle: ..., // optional
  path: ..., // optional
  scriptsJs: ..., // optional
  stylesCss: ..., // optional
};

// Call the `updatePageContentRef()` function to get a reference to the mutation.
const ref = updatePageContentRef(updatePageContentVars);
// Variables can be defined inline as well.
const ref = updatePageContentRef({ id: ..., contentHtml: ..., lastUpdated: ..., metaDescription: ..., pageTitle: ..., path: ..., scriptsJs: ..., stylesCss: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = updatePageContentRef(dataConnect, updatePageContentVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.pageContent_update);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.pageContent_update);
});
```

## DeletePageContent
You can execute the `DeletePageContent` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
deletePageContent(vars: DeletePageContentVariables): MutationPromise<DeletePageContentData, DeletePageContentVariables>;

interface DeletePageContentRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: DeletePageContentVariables): MutationRef<DeletePageContentData, DeletePageContentVariables>;
}
export const deletePageContentRef: DeletePageContentRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
deletePageContent(dc: DataConnect, vars: DeletePageContentVariables): MutationPromise<DeletePageContentData, DeletePageContentVariables>;

interface DeletePageContentRef {
  ...
  (dc: DataConnect, vars: DeletePageContentVariables): MutationRef<DeletePageContentData, DeletePageContentVariables>;
}
export const deletePageContentRef: DeletePageContentRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the deletePageContentRef:
```typescript
const name = deletePageContentRef.operationName;
console.log(name);
```

### Variables
The `DeletePageContent` mutation requires an argument of type `DeletePageContentVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface DeletePageContentVariables {
  id: UUIDString;
}
```
### Return Type
Recall that executing the `DeletePageContent` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `DeletePageContentData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface DeletePageContentData {
  pageContent_delete?: PageContent_Key | null;
}
```
### Using `DeletePageContent`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, deletePageContent, DeletePageContentVariables } from '@dataconnect/generated';

// The `DeletePageContent` mutation requires an argument of type `DeletePageContentVariables`:
const deletePageContentVars: DeletePageContentVariables = {
  id: ..., 
};

// Call the `deletePageContent()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await deletePageContent(deletePageContentVars);
// Variables can be defined inline as well.
const { data } = await deletePageContent({ id: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await deletePageContent(dataConnect, deletePageContentVars);

console.log(data.pageContent_delete);

// Or, you can use the `Promise` API.
deletePageContent(deletePageContentVars).then((response) => {
  const data = response.data;
  console.log(data.pageContent_delete);
});
```

### Using `DeletePageContent`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, deletePageContentRef, DeletePageContentVariables } from '@dataconnect/generated';

// The `DeletePageContent` mutation requires an argument of type `DeletePageContentVariables`:
const deletePageContentVars: DeletePageContentVariables = {
  id: ..., 
};

// Call the `deletePageContentRef()` function to get a reference to the mutation.
const ref = deletePageContentRef(deletePageContentVars);
// Variables can be defined inline as well.
const ref = deletePageContentRef({ id: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = deletePageContentRef(dataConnect, deletePageContentVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.pageContent_delete);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.pageContent_delete);
});
```

