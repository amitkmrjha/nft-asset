# nft-asset
pekko grpc event source cluster


# NFTAsset Service (nft-asset)
NFTAsset Service.


### Running `nft-asset` service from terminal
The project can be run in development mode by issuing the following command from project root directory.

The sbt-revolver plugin can also be used to fork the process to run the project.

```shell
sbt
```
* Navigate to `nft-asset-svc` folder

```shell
sbt:asset-svc>project nft-asset-svc
```

```shell
reStart --- -Dconfig.resource=local1.conf
reStop
```

### Executing `nft-asset`  unittests
Executing the unittests is as simple as:
```shell
sbt nft-asset/test
```

